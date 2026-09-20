package com.respondr.incident;

import com.respondr.auth.User;
import com.respondr.auth.UserRepository;
import com.respondr.common.exception.ApiException;
import com.respondr.common.exception.ErrorCode;
import com.respondr.common.exception.ResourceNotFoundException;
import com.respondr.common.security.TenantContextHolder;
import com.respondr.incident.dto.*;
import com.respondr.organization.Organization;
import com.respondr.organization.OrganizationRepository;
import com.respondr.servicecatalog.ServiceEntity;
import com.respondr.servicecatalog.ServiceRepository;
import com.respondr.team.Team;
import com.respondr.team.TeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final CommentRepository commentRepository;
    private final OrganizationRepository organizationRepository;
    private final ServiceRepository serviceRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final com.respondr.event.OutboxService outboxService;

    public IncidentService(
            IncidentRepository incidentRepository,
            IncidentEventRepository incidentEventRepository,
            CommentRepository commentRepository,
            OrganizationRepository organizationRepository,
            ServiceRepository serviceRepository,
            TeamRepository teamRepository,
            UserRepository userRepository,
            com.respondr.event.OutboxService outboxService) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.commentRepository = commentRepository;
        this.organizationRepository = organizationRepository;
        this.serviceRepository = serviceRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.outboxService = outboxService;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Page<IncidentResponse> listIncidents(Pageable pageable) {
        UUID currentOrgId = TenantContextHolder.getRequiredOrgId();
        return incidentRepository.findByOrganizationId(currentOrgId, pageable)
                .map(IncidentResponse::from);
    }

    public IncidentResponse getById(UUID id) {
        Incident incident = getRequiredIncident(id);
        return IncidentResponse.from(incident);
    }

    public List<IncidentEventResponse> getTimeline(UUID id) {
        Incident incident = getRequiredIncident(id);
        return incidentEventRepository.findByIncidentIdOrderByCreatedAtDesc(incident.getId())
                .stream()
                .map(IncidentEventResponse::from)
                .toList();
    }

    public List<CommentResponse> getComments(UUID id) {
        Incident incident = getRequiredIncident(id);
        return commentRepository.findByIncidentIdOrderByCreatedAtAsc(incident.getId())
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    @Transactional
    public IncidentResponse create(CreateIncidentRequest req) {
        UUID currentOrgId = TenantContextHolder.getRequiredOrgId();
        Organization org = organizationRepository.findById(currentOrgId)
                .orElseThrow(() -> ResourceNotFoundException.organization(currentOrgId.toString()));

        ServiceEntity service = null;
        if (req.serviceId() != null) {
            service = serviceRepository.findById(req.serviceId())
                    .orElseThrow(() -> ResourceNotFoundException.service(req.serviceId().toString()));
            assertOrgAccess(service.getOrganization().getId());
        }

        Team team = null;
        if (req.teamId() != null) {
            team = teamRepository.findById(req.teamId())
                    .orElseThrow(() -> ResourceNotFoundException.team(req.teamId().toString()));
            assertOrgAccess(team.getOrganization().getId());
        }

        Incident incident = new Incident(org, req.title(), req.severity());
        incident.setDescription(req.description());
        incident.setService(service);
        incident.setTeam(team);
        incident.setSource("MANUAL");

        incident = incidentRepository.save(incident);

        outboxService.publishEvent(
                "INCIDENT",
                incident.getId().toString(),
                "IncidentCreated",
                org.getId(),
                java.util.Map.of("incidentId", incident.getId(), "title", incident.getTitle(), "severity", incident.getSeverity().name(), "status", incident.getStatus().name())
        );

        User actor = getCurrentUser();
        IncidentEvent event = new IncidentEvent(incident, "INCIDENT_CREATED_MANUALLY", actor, null);
        incidentEventRepository.save(event);

        return IncidentResponse.from(incident);
    }

    @Transactional
    public IncidentResponse acknowledge(UUID id) {
        return transitionState(id, IncidentStatus.ACKNOWLEDGED);
    }

    @Transactional
    public IncidentResponse investigate(UUID id) {
        return transitionState(id, IncidentStatus.INVESTIGATING);
    }

    @Transactional
    public IncidentResponse resolve(UUID id) {
        return transitionState(id, IncidentStatus.RESOLVED);
    }

    @Transactional
    public IncidentResponse reopen(UUID id) {
        return transitionState(id, IncidentStatus.REOPENED);
    }

    @Transactional
    public IncidentResponse cancel(UUID id) {
        return transitionState(id, IncidentStatus.CANCELLED);
    }

    @Transactional
    public CommentResponse addComment(UUID id, CommentRequest req) {
        Incident incident = getRequiredIncident(id);
        User author = getCurrentUser();

        Comment comment = new Comment(incident, author, req.body());
        comment = commentRepository.save(comment);

        java.util.Map<String, Object> commentPayload = new java.util.HashMap<>();
        commentPayload.put("incidentId", incident.getId());
        if (comment.getId() != null) commentPayload.put("commentId", comment.getId());
        commentPayload.put("authorId", (author != null && author.getId() != null) ? author.getId().toString() : "system");
        if (comment.getBody() != null) commentPayload.put("body", comment.getBody());

        outboxService.publishEvent(
                "INCIDENT",
                incident.getId().toString(),
                "CommentAdded",
                incident.getOrganization().getId(),
                commentPayload
        );

        IncidentEvent event = new IncidentEvent(
                incident,
                "COMMENT_ADDED",
                author,
                "{\"commentId\":\"" + comment.getId() + "\"}"
        );
        incidentEventRepository.save(event);

        return CommentResponse.from(comment);
    }

    // ── State Machine Transition Enforcement ──────────────────────────────────

    @Transactional
    public IncidentResponse transitionState(UUID id, IncidentStatus targetStatus) {
        Incident incident = getRequiredIncident(id);
        IncidentStatus currentStatus = incident.getStatus();

        validateTransition(currentStatus, targetStatus);

        incident.setStatus(targetStatus);
        if (targetStatus == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(OffsetDateTime.now());
        } else if (currentStatus == IncidentStatus.RESOLVED) {
            incident.setResolvedAt(null);
        }

        incident = incidentRepository.save(incident);

        String eventType = switch (targetStatus) {
            case ACKNOWLEDGED -> "IncidentAcknowledged";
            case RESOLVED -> "IncidentResolved";
            case REOPENED -> "IncidentReopened";
            default -> null;
        };

        if (eventType != null) {
            outboxService.publishEvent(
                    "INCIDENT",
                    incident.getId().toString(),
                    eventType,
                    incident.getOrganization().getId(),
                    java.util.Map.of("incidentId", incident.getId(), "previousStatus", currentStatus.name(), "newStatus", targetStatus.name())
            );
        }

        User actor = getCurrentUser();
        IncidentEvent event = new IncidentEvent(
                incident,
                "STATUS_TRANSITION_" + targetStatus.name(),
                actor,
                "{\"from\":\"" + currentStatus.name() + "\",\"to\":\"" + targetStatus.name() + "\"}"
        );
        incidentEventRepository.save(event);

        return IncidentResponse.from(incident);
    }

    public static void validateTransition(IncidentStatus from, IncidentStatus to) {
        if (from == to) return;

        boolean valid = switch (from) {
            case OPEN -> to == IncidentStatus.ACKNOWLEDGED || to == IncidentStatus.CANCELLED;
            case ACKNOWLEDGED -> to == IncidentStatus.INVESTIGATING || to == IncidentStatus.RESOLVED;
            case INVESTIGATING -> to == IncidentStatus.RESOLVED;
            case RESOLVED -> to == IncidentStatus.REOPENED;
            case REOPENED -> to == IncidentStatus.ACKNOWLEDGED || to == IncidentStatus.INVESTIGATING;
            case CANCELLED, CLOSED -> false;
        };

        if (!valid) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    ErrorCode.BAD_REQUEST,
                    "Invalid status transition from " + from + " to " + to
            );
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Incident getRequiredIncident(UUID id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND, "Incident not found: " + id));
        assertOrgAccess(incident.getOrganization().getId());
        return incident;
    }

    private void assertOrgAccess(UUID orgId) {
        UUID currentOrgId = TenantContextHolder.getRequiredOrgId();
        if (!currentOrgId.equals(orgId)) {
            throw new AccessDeniedException("Access denied to organization resources: " + orgId);
        }
    }

    private User getCurrentUser() {
        UUID userId = TenantContextHolder.getRequiredUserId();
        return userRepository.findById(userId).orElse(null);
    }
}
