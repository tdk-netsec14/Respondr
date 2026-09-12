package com.respondr.escalation;

import com.respondr.common.redis.RedisService;
import com.respondr.escalation.dto.*;
import com.respondr.incident.*;
import com.respondr.organization.Organization;
import com.respondr.organization.OrganizationRepository;
import com.respondr.team.Team;
import com.respondr.team.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EscalationServiceTest {

    @Mock private EscalationPolicyRepository policyRepository;
    @Mock private EscalationStepRepository stepRepository;
    @Mock private IncidentEscalationStateRepository stateRepository;
    @Mock private OrganizationRepository orgRepository;
    @Mock private TeamRepository teamRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentEventRepository incidentEventRepository;
    @Mock private RedisService redisService;
    @Mock private com.respondr.event.OutboxService outboxService;

    @InjectMocks private EscalationService escalationService;

    private Organization org;
    private Team team;
    private EscalationPolicy policy;
    private Incident incident;

    @BeforeEach
    void setUp() {
        org = new Organization("Acme Corp", "acme-corp");
        team = new Team(org, "Platform", "Platform Team");
        policy = new EscalationPolicy(org, team, "Default Escalation");
        incident = new Incident(org, "Server Down", IncidentSeverity.CRITICAL);

        lenient().when(redisService.acquireLock(anyString(), anyString(), anyLong())).thenReturn(true);
    }

    @Test
    void createPolicy_createsAndReturnsPolicy() {
        UUID orgId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        when(orgRepository.findById(orgId)).thenReturn(Optional.of(org));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(policyRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PolicyResponse resp = escalationService.createPolicy(orgId, new CreatePolicyRequest(teamId, "Policy 1"));

        assertThat(resp.name()).isEqualTo("Policy 1");
        verify(policyRepository).save(any());
    }

    @Test
    void executeEscalationStep_haltsWhenIncidentAcknowledged() {
        UUID incidentId = UUID.randomUUID();
        incident.setStatus(IncidentStatus.ACKNOWLEDGED);
        IncidentEscalationState state = new IncidentEscalationState(incident, policy);

        when(stateRepository.findByIncidentId(incidentId)).thenReturn(Optional.of(state));

        boolean executed = escalationService.executeEscalationStep(incidentId, 1);

        assertThat(executed).isFalse();
        assertThat(state.getStatus()).isEqualTo(EscalationStatus.HALTED);
        verify(stateRepository).save(state);
    }

    @Test
    void executeEscalationStep_executesStepAndSchedulesNext() {
        UUID incidentId = UUID.randomUUID();
        IncidentEscalationState state = new IncidentEscalationState(incident, policy);

        EscalationStep step1 = new EscalationStep(policy, 1, 300, TargetType.ON_CALL_USER, null);
        EscalationStep step2 = new EscalationStep(policy, 2, 600, TargetType.TEAM, null);

        when(stateRepository.findByIncidentId(incidentId)).thenReturn(Optional.of(state));
        when(stepRepository.findByPolicyIdOrderByStepOrderAsc(any())).thenReturn(List.of(step1, step2));

        boolean executed = escalationService.executeEscalationStep(incidentId, 1);

        assertThat(executed).isTrue();
        assertThat(state.getCurrentStepOrder()).isEqualTo(1);
        assertThat(state.getStatus()).isEqualTo(EscalationStatus.PENDING);
        assertThat(state.getNextExecutionAt()).isNotNull();
        verify(incidentEventRepository).save(any());
        verify(redisService).releaseLock(anyString(), anyString());
    }

    @Test
    void executeEscalationStep_completesWhenLastStepExecuted() {
        UUID incidentId = UUID.randomUUID();
        IncidentEscalationState state = new IncidentEscalationState(incident, policy);

        EscalationStep step1 = new EscalationStep(policy, 1, 300, TargetType.ON_CALL_USER, null);

        when(stateRepository.findByIncidentId(incidentId)).thenReturn(Optional.of(state));
        when(stepRepository.findByPolicyIdOrderByStepOrderAsc(any())).thenReturn(List.of(step1));

        boolean executed = escalationService.executeEscalationStep(incidentId, 1);

        assertThat(executed).isTrue();
        assertThat(state.getCurrentStepOrder()).isEqualTo(1);
        assertThat(state.getStatus()).isEqualTo(EscalationStatus.COMPLETED);
        assertThat(state.getNextExecutionAt()).isNull();
    }
}
