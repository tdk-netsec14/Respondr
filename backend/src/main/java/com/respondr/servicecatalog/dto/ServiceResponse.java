package com.respondr.servicecatalog.dto;

import com.respondr.servicecatalog.ServiceEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Response DTO for a service. */
public record ServiceResponse(
        UUID id,
        UUID orgId,
        UUID teamId,
        String name,
        String key,
        String description,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ServiceResponse from(ServiceEntity s) {
        return new ServiceResponse(
                s.getId(),
                s.getOrganization().getId(),
                s.getTeam() != null ? s.getTeam().getId() : null,
                s.getName(),
                s.getKey(),
                s.getDescription(),
                s.isActive(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }
}
