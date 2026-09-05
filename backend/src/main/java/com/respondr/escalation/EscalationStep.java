package com.respondr.escalation;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Single step within an escalation policy.
 */
@Entity
@Table(
    name = "escalation_steps",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_escalation_step_policy_order",
        columnNames = {"policy_id", "step_order"}
    )
)
public class EscalationStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private EscalationPolicy policy;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(name = "delay_seconds", nullable = false)
    private int delaySeconds = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private TargetType targetType = TargetType.ON_CALL_USER;

    @Column(name = "target_reference")
    private UUID targetReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected EscalationStep() {}

    public EscalationStep(EscalationPolicy policy, int stepOrder, int delaySeconds, TargetType targetType, UUID targetReference) {
        this.policy = policy;
        this.stepOrder = stepOrder;
        this.delaySeconds = delaySeconds;
        this.targetType = targetType;
        this.targetReference = targetReference;
    }

    public UUID getId()                 { return id; }
    public EscalationPolicy getPolicy() { return policy; }
    public int getStepOrder()           { return stepOrder; }
    public int getDelaySeconds()        { return delaySeconds; }
    public TargetType getTargetType()   { return targetType; }
    public UUID getTargetReference()    { return targetReference; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setStepOrder(int stepOrder)          { this.stepOrder = stepOrder; }
    public void setDelaySeconds(int delaySeconds)    { this.delaySeconds = delaySeconds; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }
    public void setTargetReference(UUID targetRef)   { this.targetReference = targetRef; }
}
