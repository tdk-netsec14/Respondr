package com.respondr.oncall.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ResponderResponse(
        UUID userId,
        String userName,
        String userEmail,
        UUID scheduleId,
        String scheduleName,
        boolean isOverride,
        OffsetDateTime timestamp
) {}
