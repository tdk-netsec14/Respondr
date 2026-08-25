package com.respondr.oncall.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ShiftDto(
        UUID scheduleId,
        UUID userId,
        String userName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        boolean isOverride
) {}
