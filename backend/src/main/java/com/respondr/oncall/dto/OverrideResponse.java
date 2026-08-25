package com.respondr.oncall.dto;

import com.respondr.oncall.ScheduleOverride;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OverrideResponse(
        UUID id,
        UUID scheduleId,
        UUID userId,
        String userName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String reason,
        OffsetDateTime createdAt
) {
    public static OverrideResponse from(ScheduleOverride override) {
        return new OverrideResponse(
                override.getId(),
                override.getSchedule().getId(),
                override.getUser().getId(),
                override.getUser().getName(),
                override.getStartAt(),
                override.getEndAt(),
                override.getReason(),
                override.getCreatedAt()
        );
    }
}
