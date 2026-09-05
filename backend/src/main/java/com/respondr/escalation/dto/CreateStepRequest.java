package com.respondr.escalation.dto;

import com.respondr.escalation.TargetType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateStepRequest(
        @Min(value = 1, message = "stepOrder must be at least 1")
        int stepOrder,

        @Min(value = 0, message = "delaySeconds cannot be negative")
        int delaySeconds,

        @NotNull(message = "targetType must not be null")
        TargetType targetType,

        UUID targetReference
) {}
