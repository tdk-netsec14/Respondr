package com.respondr.incident.dto;

import com.respondr.incident.Incident;
import com.respondr.incident.IncidentSeverity;
import com.respondr.incident.IncidentStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        UUID orgId,
        UUID serviceId,
        UUID teamId,
        String title,
        String description,
        IncidentSeverity severity,
        IncidentStatus status,
        String source,
        UUID assigneeId,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime resolvedAt
) {
    public static IncidentResponse from(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getOrganization().getId(),
                incident.getService() != null ? incident.getService().getId() : null,
                incident.getTeam() != null ? incident.getTeam().getId() : null,
                incident.getTitle(),
                incident.getDescription(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getSource(),
                incident.getAssignee() != null ? incident.getAssignee().getId() : null,
                incident.getCreatedAt(),
                incident.getUpdatedAt(),
                incident.getResolvedAt()
        );
    }
}
