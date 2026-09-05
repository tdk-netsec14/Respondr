package com.respondr.escalation;

import com.respondr.common.persistence.BaseEntity;
import com.respondr.incident.Incident;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Execution tracking entity for an incident undergoing escalation.
 */
@Entity
@Table(name = "incident_escalation_states")
public class IncidentEscalationState extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false, unique = true)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private EscalationPolicy policy;

    @Column(name = "current_step_order", nullable = false)
    private int currentStepOrder = 0;

    @Column(name = "next_execution_at")
    private OffsetDateTime nextExecutionAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EscalationStatus status = EscalationStatus.PENDING;

    protected IncidentEscalationState() {}

    public IncidentEscalationState(Incident incident, EscalationPolicy policy) {
        this.incident = incident;
        this.policy = policy;
        this.currentStepOrder = 0;
        this.status = EscalationStatus.PENDING;
    }

    public Incident getIncident()                   { return incident; }
    public EscalationPolicy getPolicy()             { return policy; }
    public int getCurrentStepOrder()                { return currentStepOrder; }
    public OffsetDateTime getNextExecutionAt()      { return nextExecutionAt; }
    public EscalationStatus getStatus()             { return status; }

    public void setCurrentStepOrder(int stepOrder)  { this.currentStepOrder = stepOrder; }
    public void setNextExecutionAt(OffsetDateTime t){ this.nextExecutionAt = t; }
    public void setStatus(EscalationStatus status)  { this.status = status; }
}
