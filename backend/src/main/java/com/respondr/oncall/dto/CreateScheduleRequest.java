package com.respondr.oncall.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CreateScheduleRequest(
        @NotBlank(message = "name must not be blank")
        String name,

        @NotBlank(message = "timezone must not be blank")
        String timezone,

        @NotEmpty(message = "participantUserIds must not be empty")
        List<UUID> participantUserIds,

        @Positive(message = "shiftLengthHours must be positive")
        long shiftLengthHours,

        @NotNull(message = "rotationStartAt must not be null")
        OffsetDateTime rotationStartAt
) {}
