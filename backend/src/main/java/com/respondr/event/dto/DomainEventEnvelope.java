package com.respondr.event.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Standard envelope format for all asynchronous Respondr domain events.
 */
public record DomainEventEnvelope(
        UUID eventId,
        String eventType,
        String aggregateId,
        UUID organizationId,
        Instant occurredAt,
        int schemaVersion,
        Map<String, Object> payload
) {
    public static DomainEventEnvelope create(String eventType, String aggregateId, UUID organizationId, Map<String, Object> payload) {
        return new DomainEventEnvelope(
                UUID.randomUUID(),
                eventType,
                aggregateId,
                organizationId,
                Instant.now(),
                1,
                payload
        );
    }
}
