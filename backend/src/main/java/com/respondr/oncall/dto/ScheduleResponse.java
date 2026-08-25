package com.respondr.oncall.dto;

import com.respondr.oncall.OnCallSchedule;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleResponse(
        UUID id,
        UUID orgId,
        UUID teamId,
        String teamName,
        String name,
        String timezone,
        String rotationRules,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ScheduleResponse from(OnCallSchedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getOrganization().getId(),
                schedule.getTeam().getId(),
                schedule.getTeam().getName(),
                schedule.getName(),
                schedule.getTimezone(),
                schedule.getRotationRules(),
                schedule.isActive(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
