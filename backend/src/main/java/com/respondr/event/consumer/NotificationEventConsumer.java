package com.respondr.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventConsumer.class);
    private static final String GROUP_ID = "respondr-notification-group";

    private final IdempotencyTracker idempotencyTracker;
    private final ObjectMapper objectMapper;

    public NotificationEventConsumer(IdempotencyTracker idempotencyTracker, ObjectMapper objectMapper) {
        this.idempotencyTracker = idempotencyTracker;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"respondr.incidents", "respondr.comments", "respondr.alerts"}, groupId = GROUP_ID, autoStartup = "${spring.kafka.listener.auto-startup:true}")
    public void consume(String rawMessage) {
        try {
            JsonNode root = objectMapper.readTree(rawMessage);
            String eventId = root.path("eventId").asText();
            String eventType = root.path("eventType").asText();

            if (!idempotencyTracker.markIfNew(eventId, GROUP_ID)) {
                log.info("[Notification] Duplicate event {} received, skipping idempotently", eventId);
                return;
            }

            log.info("[Notification] Processing event: type={}, id={}", eventType, eventId);
            // Skeleton placeholder for Phase 8 notification dispatch logic
        } catch (Exception e) {
            log.error("[Notification] Failed to process message: {}", e.getMessage());
        }
    }
}
