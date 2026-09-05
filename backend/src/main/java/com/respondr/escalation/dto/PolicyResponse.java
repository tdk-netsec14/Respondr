package com.respondr.escalation.dto;

import com.respondr.escalation.EscalationPolicy;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PolicyResponse(
        UUID id,
        UUID orgId,
        UUID teamId,
        String name,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PolicyResponse from(EscalationPolicy policy) {
        return new PolicyResponse(
                policy.getId(),
                policy.getOrganization().getId(),
                policy.getTeam().getId(),
                policy.getName(),
                policy.isActive(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }
}
