package com.respondr.escalation;

import com.respondr.common.exception.ConflictException;
import com.respondr.common.exception.ErrorCode;
import com.respondr.common.exception.ResourceNotFoundException;
import com.respondr.common.redis.RedisService;
import com.respondr.escalation.dto.*;
import com.respondr.incident.*;
import com.respondr.organization.Organization;
import com.respondr.organization.OrganizationRepository;
import com.respondr.team.Team;
import com.respondr.team.TeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EscalationService {

    private static final Logger log = LoggerFactory.getLogger(EscalationService.class);

    private final EscalationPolicyRepository policyRepository;
    private final EscalationStepRepository stepRepository;
    private final IncidentEscalationStateRepository stateRepository;
    private final OrganizationRepository orgRepository;
    private final TeamRepository teamRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final RedisService redisService;
    private final com.respondr.event.OutboxService outboxService;

    public EscalationService(
            EscalationPolicyRepository policyRepository,
            EscalationStepRepository stepRepository,
            IncidentEscalationStateRepository stateRepository,
            OrganizationRepository orgRepository,
            TeamRepository teamRepository,
            IncidentRepository incidentRepository,
            IncidentEventRepository incidentEventRepository,
            RedisService redisService,
            com.respondr.event.OutboxService outboxService) {
        this.policyRepository = policyRepository;
        this.stepRepository = stepRepository;
        this.stateRepository = stateRepository;
        this.orgRepository = orgRepository;
        this.teamRepository = teamRepository;
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.redisService = redisService;
        this.outboxService = outboxService;
    }

    // ── Policy Management ────────────────────────────────────────────────────

    public PolicyResponse createPolicy(UUID orgId, CreatePolicyRequest req) {
        Organization org = orgRepository.findById(orgId)
                .orElseThrow(() -> ResourceNotFoundException.organization(orgId.toString()));
        Team team = teamRepository.findById(req.teamId())
                .orElseThrow(() -> ResourceNotFoundException.team(req.teamId().toString()));

        EscalationPolicy policy = new EscalationPolicy(org, team, req.name());
        return PolicyResponse.from(policyRepository.save(policy));
    }

    @Transactional(readOnly = true)
    public PolicyResponse getPolicy(UUID orgId, UUID policyId) {
        EscalationPolicy policy = policyRepository.findByIdAndOrganizationId(policyId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ESCALATION_POLICY_NOT_FOUND, "Escalation policy not found: " + policyId));
        return PolicyResponse.from(policy);
    }

    @Transactional(readOnly = true)
    public Page<PolicyResponse> listPolicies(UUID orgId, Pageable pageable) {
        return policyRepository.findByOrganizationId(orgId, pageable).map(PolicyResponse::from);
    }

    public PolicyResponse updatePolicy(UUID orgId, UUID policyId, UpdatePolicyRequest req) {
        EscalationPolicy policy = policyRepository.findByIdAndOrganizationId(policyId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ESCALATION_POLICY_NOT_FOUND, "Escalation policy not found: " + policyId));
        policy.setName(req.name());
        policy.setActive(req.active());
        return PolicyResponse.from(policyRepository.save(policy));
    }

    public void deletePolicy(UUID orgId, UUID policyId) {
        EscalationPolicy policy = policyRepository.findByIdAndOrganizationId(policyId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ESCALATION_POLICY_NOT_FOUND, "Escalation policy not found: " + policyId));
        policyRepository.delete(policy);
    }

    // ── Step Management ───────────────────────────────────────────────────────

    public StepResponse addStep(UUID orgId, UUID policyId, CreateStepRequest req) {
        EscalationPolicy policy = policyRepository.findByIdAndOrganizationId(policyId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ESCALATION_POLICY_NOT_FOUND, "Escalation policy not found: " + policyId));

        if (stepRepository.findByPolicyIdAndStepOrder(policyId, req.stepOrder()).isPresent()) {
            throw new ConflictException(ErrorCode.CONFLICT, "Step order " + req.stepOrder() + " already exists for policy " + policyId);
        }

        EscalationStep step = new EscalationStep(policy, req.stepOrder(), req.delaySeconds(), req.targetType(), req.targetReference());
        return StepResponse.from(stepRepository.save(step));
    }

    @Transactional(readOnly = true)
    public List<StepResponse> getSteps(UUID orgId, UUID policyId) {
        policyRepository.findByIdAndOrganizationId(policyId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ESCALATION_POLICY_NOT_FOUND, "Escalation policy not found: " + policyId));
        return stepRepository.findByPolicyIdOrderByStepOrderAsc(policyId).stream()
                .map(StepResponse::from)
                .toList();
    }

    public void deleteStep(UUID orgId, UUID policyId, UUID stepId) {
        policyRepository.findByIdAndOrganizationId(policyId, orgId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ESCALATION_POLICY_NOT_FOUND, "Escalation policy not found: " + policyId));
        EscalationStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ESCALATION_STEP_NOT_FOUND, "Escalation step not found: " + stepId));
        stepRepository.delete(step);
    }

    // ── Escalation Execution ──────────────────────────────────────────────────

    public EscalationStateResponse initiateEscalation(Incident incident, EscalationPolicy policy) {
        IncidentEscalationState state = new IncidentEscalationState(incident, policy);
        List<EscalationStep> steps = stepRepository.findByPolicyIdOrderByStepOrderAsc(policy.getId());
        if (!steps.isEmpty()) {
            EscalationStep firstStep = steps.get(0);
            state.setNextExecutionAt(OffsetDateTime.now().plusSeconds(firstStep.getDelaySeconds()));
            state.setStatus(EscalationStatus.PENDING);
        } else {
            state.setStatus(EscalationStatus.COMPLETED);
        }
        return EscalationStateResponse.from(stateRepository.save(state));
    }

    public boolean executeEscalationStep(UUID incidentId, int stepOrder) {
        String lockKey = "escalation:step:" + incidentId + ":" + stepOrder;
        String lockVal = UUID.randomUUID().toString();

        if (!redisService.acquireLock(lockKey, lockVal, 30)) {
            log.info("Could not acquire lock for escalation step execution: {}", lockKey);
            return false;
        }

        try {
            Optional<IncidentEscalationState> stateOpt = stateRepository.findByIncidentId(incidentId);
            if (stateOpt.isEmpty()) {
                log.warn("No escalation state found for incident {}", incidentId);
                return false;
            }

            IncidentEscalationState state = stateOpt.get();
            Incident incident = state.getIncident();

            // Check if incident is resolved / acknowledged / closed
            if (incident.getStatus() == IncidentStatus.ACKNOWLEDGED ||
                incident.getStatus() == IncidentStatus.RESOLVED ||
                incident.getStatus() == IncidentStatus.CLOSED) {
                log.info("Incident {} is already {}, halting escalation", incidentId, incident.getStatus());
                state.setStatus(EscalationStatus.HALTED);
                stateRepository.save(state);
                return false;
            }

            // Check idempotency: avoid re-running step
            if (state.getCurrentStepOrder() >= stepOrder) {
                log.info("Escalation step {} for incident {} already executed (current step: {})", stepOrder, incidentId, state.getCurrentStepOrder());
                return false;
            }

            // Execute step
            state.setCurrentStepOrder(stepOrder);
            state.setStatus(EscalationStatus.IN_PROGRESS);

            UUID orgId = (state.getPolicy() != null && state.getPolicy().getOrganization() != null)
                    ? state.getPolicy().getOrganization().getId() : null;
            UUID policyId = state.getPolicy() != null ? state.getPolicy().getId() : null;

            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("incidentId", incidentId);
            payload.put("stepOrder", stepOrder);
            if (policyId != null) payload.put("policyId", policyId);

            if (orgId != null) {
                outboxService.publishEvent("INCIDENT", incidentId.toString(), "IncidentEscalated", orgId, payload);
            }

            IncidentEvent event = new IncidentEvent(
                    incident,
                    "ESCALATION_STEP_EXECUTED",
                    null,
                    "{\"stepOrder\":" + stepOrder + ",\"policyId\":\"" + state.getPolicy().getId() + "\"}"
            );
            incidentEventRepository.save(event);

            // Schedule next step if any
            List<EscalationStep> steps = stepRepository.findByPolicyIdOrderByStepOrderAsc(state.getPolicy().getId());
            Optional<EscalationStep> nextStepOpt = steps.stream().filter(s -> s.getStepOrder() > stepOrder).findFirst();

            if (nextStepOpt.isPresent()) {
                EscalationStep nextStep = nextStepOpt.get();
                state.setNextExecutionAt(OffsetDateTime.now().plusSeconds(nextStep.getDelaySeconds()));
                state.setStatus(EscalationStatus.PENDING);
            } else {
                state.setNextExecutionAt(null);
                state.setStatus(EscalationStatus.COMPLETED);
            }

            stateRepository.save(state);
            return true;
        } finally {
            redisService.releaseLock(lockKey, lockVal);
        }
    }
}
