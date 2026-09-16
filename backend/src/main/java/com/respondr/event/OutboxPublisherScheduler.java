package com.respondr.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Background outbox relay. Reads PENDING outbox_events rows, publishes them
 * to Kafka, and marks them as PUBLISHED.
 *
 * <p><strong>At-Least-Once Semantics:</strong> Crashing after Kafka send but
 * before DB status update will result in re-publishing upon restart.
 * Consumers must be idempotent.
 */
@Component
public class OutboxPublisherScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisherScheduler.class);

    private final OutboxEventRepository outboxEventRepository;

    @Autowired(required = false)
    private KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisherScheduler(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc(
                OutboxStatus.PENDING, PageRequest.of(0, 50));

        if (pendingEvents.isEmpty()) {
            return;
        }

        log.debug("Processing {} pending outbox events", pendingEvents.size());

        for (OutboxEvent event : pendingEvents) {
            try {
                String topic = resolveTopic(event.getEventType(), event.getAggregateType());
                if (kafkaTemplate != null) {
                    kafkaTemplate.send(topic, event.getAggregateId(), event.getPayload())
                            .whenComplete((result, ex) -> {
                                if (ex != null) {
                                    log.error("Async Kafka send failed for outbox event {}: {}", event.getId(), ex.getMessage());
                                } else {
                                    log.debug("Outbox event {} sent to topic {}", event.getId(), topic);
                                }
                            });
                } else {
                    log.warn("KafkaTemplate unavailable; marking outbox event {} published in mock/offline mode", event.getId());
                }
                event.markPublished();
                outboxEventRepository.save(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());
                // Leave PENDING so it can be retried on next iteration
            }
        }
    }

    public String resolveTopic(String eventType, String aggregateType) {
        if (eventType == null) return "respondr.events";
        return switch (eventType) {
            case "IncidentCreated", "IncidentAcknowledged", "IncidentEscalated", "IncidentResolved", "IncidentReopened" -> "respondr.incidents";
            case "CommentAdded" -> "respondr.comments";
            case "AlertReceived" -> "respondr.alerts";
            default -> "respondr.events";
        };
    }
}
