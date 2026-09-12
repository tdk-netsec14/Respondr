package com.respondr.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RealtimeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RealtimeEventConsumer.class);
    private static final String GROUP_ID = "respondr-realtime-group";

    private final IdempotencyTracker idempotencyTracker;
    private final ObjectMapper objectMapper;

    public RealtimeEventConsumer(IdempotencyTracker idempotencyTracker, ObjectMapper objectMapper) {
        this.idempotencyTracker = idempotencyTracker;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"respondr.comments", "respondr.incidents"}, groupId = GROUP_ID, autoStartup = "${spring.kafka.listener.auto-startup:true}")
    public void consume(String rawMessage) {
        try {
            JsonNode root = objectMapper.readTree(rawMessage);
            String eventId = root.path("eventId").asText();
            String eventType = root.path("eventType").asText();

            if (!idempotencyTracker.markIfNew(eventId, GROUP_ID)) {
                log.info("[Realtime] Duplicate event {} received, skipping idempotently", eventId);
                return;
            }

            log.info("[Realtime] Broadcasting STOMP websocket update: type={}, id={}", eventType, eventId);
            // Skeleton placeholder for Phase 8 STOMP WebSocket broadcasting logic
        } catch (Exception e) {
            log.error("[Realtime] Failed to process message: {}", e.getMessage());
        }
    }
}
