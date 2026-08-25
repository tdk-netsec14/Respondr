package com.respondr.oncall;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.respondr.auth.User;
import com.respondr.auth.UserRepository;
import com.respondr.common.exception.ApiException;
import com.respondr.common.exception.ConflictException;
import com.respondr.common.exception.ErrorCode;
import com.respondr.common.exception.ResourceNotFoundException;
import com.respondr.common.security.TenantContextHolder;
import com.respondr.oncall.dto.*;
import com.respondr.organization.Organization;
import com.respondr.organization.OrganizationRepository;
import com.respondr.team.Team;
import com.respondr.team.TeamRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OnCallService {

    private final OnCallScheduleRepository scheduleRepository;
    private final ScheduleOverrideRepository overrideRepository;
    private final OrganizationRepository organizationRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public OnCallService(
            OnCallScheduleRepository scheduleRepository,
            ScheduleOverrideRepository overrideRepository,
            OrganizationRepository organizationRepository,
            TeamRepository teamRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.scheduleRepository = scheduleRepository;
        this.overrideRepository = overrideRepository;
        this.organizationRepository = organizationRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public Page<ScheduleResponse> listSchedules(Pageable pageable) {
        UUID orgId = TenantContextHolder.getRequiredOrgId();
        return scheduleRepository.findByOrganizationId(orgId, pageable)
                .map(ScheduleResponse::from);
    }

    public ScheduleResponse getById(UUID id) {
        OnCallSchedule schedule = getRequiredSchedule(id);
        return ScheduleResponse.from(schedule);
    }

    public List<OverrideResponse> getOverrides(UUID scheduleId) {
        getRequiredSchedule(scheduleId);
        return overrideRepository.findByScheduleIdOrderByStartAtAsc(scheduleId)
                .stream()
                .map(OverrideResponse::from)
                .toList();
    }

    public ResponderResponse getCurrentResponder(UUID scheduleId) {
        OnCallSchedule schedule = getRequiredSchedule(scheduleId);
        OffsetDateTime now = OffsetDateTime.now();

        List<ScheduleOverride> activeOverrides = overrideRepository.findActiveOverridesAtTime(scheduleId, now);
        RotationRulesDto rules = parseRules(schedule.getRotationRules());

        Optional<UUID> responderUserId = OnCallCalculator.calculateResponderAt(
                rules, schedule.getTimezone(), activeOverrides, now);

        if (responderUserId.isEmpty()) {
            throw new ResourceNotFoundException(ErrorCode.NOT_FOUND, "No on-call responder configured for schedule: " + scheduleId);
        }

        User user = userRepository.findById(responderUserId.get())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND, "User not found: " + responderUserId.get()));

        boolean isOverride = !activeOverrides.isEmpty() && activeOverrides.get(0).getUser().getId().equals(user.getId());

        return new ResponderResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                schedule.getId(),
                schedule.getName(),
                isOverride,
                now
        );
    }

    public Optional<User> findCurrentResponderUserForTeam(UUID teamId) {
        List<OnCallSchedule> schedules = scheduleRepository.findByTeamIdAndActiveTrue(teamId);
        if (schedules.isEmpty()) return Optional.empty();

        OnCallSchedule schedule = schedules.get(0);
        OffsetDateTime now = OffsetDateTime.now();
        List<ScheduleOverride> activeOverrides = overrideRepository.findActiveOverridesAtTime(schedule.getId(), now);
        RotationRulesDto rules = parseRules(schedule.getRotationRules());

        Optional<UUID> userId = OnCallCalculator.calculateResponderAt(rules, schedule.getTimezone(), activeOverrides, now);
        return userId.flatMap(userRepository::findById);
    }

    // ── Commands ──────────────────────────────────────────────────────────────

    @Transactional
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MEMBER')")
    public ScheduleResponse createSchedule(UUID teamId, CreateScheduleRequest req) {
        UUID currentOrgId = TenantContextHolder.getRequiredOrgId();
        Organization org = organizationRepository.findById(currentOrgId)
                .orElseThrow(() -> ResourceNotFoundException.organization(currentOrgId.toString()));

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> ResourceNotFoundException.team(teamId.toString()));
        assertOrgAccess(team.getOrganization().getId());

        RotationRulesDto rules = new RotationRulesDto(
                req.participantUserIds(), req.shiftLengthHours(), req.rotationStartAt());
        String rulesJson = serializeRules(rules);

        OnCallSchedule schedule = new OnCallSchedule(org, team, req.name(), req.timezone(), rulesJson);
        schedule = scheduleRepository.save(schedule);

        return ScheduleResponse.from(schedule);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN', 'MEMBER')")
    public OverrideResponse createOverride(UUID scheduleId, CreateOverrideRequest req) {
        OnCallSchedule schedule = getRequiredSchedule(scheduleId);

        if (!req.endAt().isAfter(req.startAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, "endAt must be strictly after startAt");
        }

        // Interval overlap detection
        List<ScheduleOverride> overlaps = overrideRepository.findOverlappingOverrides(scheduleId, req.startAt(), req.endAt());
        if (!overlaps.isEmpty()) {
            throw new ConflictException(ErrorCode.CONFLICT, "Schedule override overlaps with an existing override interval");
        }

        User user = userRepository.findById(req.userId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND, "User not found: " + req.userId()));

        ScheduleOverride override = new ScheduleOverride(schedule, user, req.startAt(), req.endAt(), req.reason());
        override = overrideRepository.save(override);

        return OverrideResponse.from(override);
    }

    @Transactional
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public void deleteSchedule(UUID id) {
        OnCallSchedule schedule = getRequiredSchedule(id);
        scheduleRepository.delete(schedule);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private OnCallSchedule getRequiredSchedule(UUID id) {
        OnCallSchedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.NOT_FOUND, "On-call schedule not found: " + id));
        assertOrgAccess(schedule.getOrganization().getId());
        return schedule;
    }

    private void assertOrgAccess(UUID orgId) {
        UUID currentOrgId = TenantContextHolder.getRequiredOrgId();
        if (!currentOrgId.equals(orgId)) {
            throw new AccessDeniedException("Access denied to organization resources: " + orgId);
        }
    }

    private RotationRulesDto parseRules(String json) {
        try {
            return objectMapper.readValue(json, RotationRulesDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse rotation rules JSON", e);
        }
    }

    private String serializeRules(RotationRulesDto rules) {
        try {
            return objectMapper.writeValueAsString(rules);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize rotation rules JSON", e);
        }
    }
}
