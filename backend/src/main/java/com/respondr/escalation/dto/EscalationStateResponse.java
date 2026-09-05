package com.respondr.escalation.dto;

import com.respondr.escalation.EscalationStatus;
import com.respondr.escalation.IncidentEscalationState;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EscalationStateResponse(
        UUID id,
        UUID incidentId,
        UUID policyId,
        int currentStepOrder,
        OffsetDateTime nextExecutionAt,
        EscalationStatus status
) {
    public static EscalationStateResponse from(IncidentEscalationState state) {
        return new EscalationStateResponse(
                state.getId(),
                state.getIncident().getId(),
                state.getPolicy().getId(),
                state.getCurrentStepOrder(),
                state.getNextExecutionAt(),
                state.getStatus()
        );
    }
}
