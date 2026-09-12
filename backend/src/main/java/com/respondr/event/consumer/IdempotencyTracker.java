package com.respondr.event.consumer;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory idempotency deduplication tracker for consumer skeletons.
 * Prevents duplicate processing when Kafka delivers events multiple times.
 */
@Component
public class IdempotencyTracker {

    private final Set<String> processedEvents = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public boolean markIfNew(String eventId, String consumerGroup) {
        String key = consumerGroup + ":" + eventId;
        return processedEvents.add(key);
    }

    public boolean isProcessed(String eventId, String consumerGroup) {
        return processedEvents.contains(consumerGroup + ":" + eventId);
    }

    public void clear() {
        processedEvents.clear();
    }
}
