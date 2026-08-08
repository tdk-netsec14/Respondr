package com.respondr.team.dto;

import com.respondr.team.Team;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Response DTO for a team. */
public record TeamResponse(
        UUID id,
        UUID orgId,
        String name,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getOrganization().getId(),
                team.getName(),
                team.getDescription(),
                team.getCreatedAt(),
                team.getUpdatedAt()
        );
    }
}
