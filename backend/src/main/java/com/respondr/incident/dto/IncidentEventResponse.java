package com.respondr.incident.dto;

import com.respondr.incident.IncidentEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IncidentEventResponse(
        UUID id,
        UUID incidentId,
        String eventType,
        UUID actorId,
        String metadata,
        OffsetDateTime createdAt
) {
    public static IncidentEventResponse from(IncidentEvent event) {
        return new IncidentEventResponse(
                event.getId(),
                event.getIncident().getId(),
                event.getEventType(),
                event.getActor() != null ? event.getActor().getId() : null,
                event.getMetadata(),
                event.getCreatedAt()
        );
    }
}
