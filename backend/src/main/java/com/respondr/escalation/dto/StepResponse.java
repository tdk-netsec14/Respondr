package com.respondr.escalation.dto;

import com.respondr.escalation.EscalationStep;
import com.respondr.escalation.TargetType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StepResponse(
        UUID id,
        UUID policyId,
        int stepOrder,
        int delaySeconds,
        TargetType targetType,
        UUID targetReference,
        OffsetDateTime createdAt
) {
    public static StepResponse from(EscalationStep step) {
        return new StepResponse(
                step.getId(),
                step.getPolicy().getId(),
                step.getStepOrder(),
                step.getDelaySeconds(),
                step.getTargetType(),
                step.getTargetReference(),
                step.getCreatedAt()
        );
    }
}
