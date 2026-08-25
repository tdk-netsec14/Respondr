package com.respondr.oncall.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateOverrideRequest(
        @NotNull(message = "userId must not be null")
        UUID userId,

        @NotNull(message = "startAt must not be null")
        OffsetDateTime startAt,

        @NotNull(message = "endAt must not be null")
        OffsetDateTime endAt,

        @Size(max = 1000, message = "reason must be at most 1000 characters")
        String reason
) {}
