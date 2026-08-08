package com.respondr.organization.dto;

import com.respondr.organization.Organization;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Response DTO for an organization. Never exposes JPA entity objects directly. */
public record OrganizationResponse(
        UUID id,
        String name,
        String slug,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static OrganizationResponse from(Organization org) {
        return new OrganizationResponse(
                org.getId(),
                org.getName(),
                org.getSlug(),
                org.getCreatedAt(),
                org.getUpdatedAt()
        );
    }
}
