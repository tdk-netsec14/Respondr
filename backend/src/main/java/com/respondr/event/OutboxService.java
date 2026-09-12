package com.respondr.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.respondr.event.dto.DomainEventEnvelope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Service for transactional outbox write path.
 * Enforces saving OutboxEvent rows within the caller's active database transaction.
 */
@Service
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxService(OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent publishEvent(String aggregateType, String aggregateId, String eventType, UUID orgId, Map<String, Object> payload) {
        DomainEventEnvelope envelope = DomainEventEnvelope.create(eventType, aggregateId, orgId, payload);
        try {
            String jsonPayload = objectMapper.writeValueAsString(envelope);
            OutboxEvent event = new OutboxEvent(aggregateType, aggregateId, eventType, jsonPayload);
            return outboxEventRepository.save(event);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize outbox event payload for " + eventType, e);
        }
    }
}
