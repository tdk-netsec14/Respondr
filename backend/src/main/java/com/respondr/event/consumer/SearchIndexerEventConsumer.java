package com.respondr.event.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class SearchIndexerEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexerEventConsumer.class);
    private static final String GROUP_ID = "respondr-search-indexer-group";

    private final IdempotencyTracker idempotencyTracker;
    private final ObjectMapper objectMapper;

    public SearchIndexerEventConsumer(IdempotencyTracker idempotencyTracker, ObjectMapper objectMapper) {
        this.idempotencyTracker = idempotencyTracker;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = {"respondr.incidents"}, groupId = GROUP_ID, autoStartup = "${spring.kafka.listener.auto-startup:true}")
    public void consume(String rawMessage) {
        try {
            JsonNode root = objectMapper.readTree(rawMessage);
            String eventId = root.path("eventId").asText();
            String eventType = root.path("eventType").asText();

            if (!idempotencyTracker.markIfNew(eventId, GROUP_ID)) {
                log.info("[SearchIndexer] Duplicate event {} received, skipping idempotently", eventId);
                return;
            }

            log.info("[SearchIndexer] Indexing incident event: type={}, id={}", eventType, eventId);
            // Skeleton placeholder for Phase 9 Elasticsearch indexing logic
        } catch (Exception e) {
            log.error("[SearchIndexer] Failed to process message: {}", e.getMessage());
        }
    }
}
