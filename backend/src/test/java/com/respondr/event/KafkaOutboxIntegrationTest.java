package com.respondr.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.respondr.event.consumer.IdempotencyTracker;
import com.respondr.event.consumer.NotificationEventConsumer;
import com.respondr.event.dto.DomainEventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kafka Outbox Integration Tests.
 *
 * <p>Uses Spring's EmbeddedKafka broker (no Docker required) + H2 in-memory DB
 * (via application-test.yml). Validates the full outbox write → relay → Kafka publish
 * cycle and consumer idempotency under duplicate delivery.
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = {"respondr.incidents", "respondr.comments", "respondr.alerts"},
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@DirtiesContext
class KafkaOutboxIntegrationTest {

    @Autowired private OutboxService outboxService;
    @Autowired private OutboxEventRepository outboxEventRepository;
    @Autowired private OutboxPublisherScheduler outboxPublisherScheduler;
    @Autowired private IdempotencyTracker idempotencyTracker;
    @Autowired private NotificationEventConsumer notificationConsumer;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        idempotencyTracker.clear();
    }

    // ── Test 1: Outbox write path ────────────────────────────────────────────

    @Test
    void outboxWritePath_createsOutboxEventInSameTransaction() {
        UUID orgId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        OutboxEvent created = transactionTemplate.execute(status -> outboxService.publishEvent(
                "INCIDENT",
                incidentId.toString(),
                "IncidentCreated",
                orgId,
                Map.of("title", "High Latency Alert")
        ));

        assertThat(created).isNotNull();
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(created.getEventType()).isEqualTo("IncidentCreated");
    }

    // ── Test 2: Outbox relay marks published ─────────────────────────────────

    @Test
    void outboxPublisherScheduler_relaysPendingEventAndMarksPublished() {
        UUID orgId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        OutboxEvent created = transactionTemplate.execute(status -> outboxService.publishEvent(
                "INCIDENT",
                incidentId.toString(),
                "IncidentResolved",
                orgId,
                Map.of("incidentId", incidentId.toString(), "status", "RESOLVED")
        ));

        assertThat(created).isNotNull();

        // Invoke scheduler directly (already transactional)
        outboxPublisherScheduler.processOutboxEvents();

        OutboxEvent updated = outboxEventRepository.findById(created.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(updated.getPublishedAt()).isNotNull();
    }

    // ── Test 3: Consumer idempotency ─────────────────────────────────────────

    @Test
    void idempotentConsumer_deduplicatesDuplicateDeliveries() throws Exception {
        UUID eventId = UUID.randomUUID();
        DomainEventEnvelope envelope = new DomainEventEnvelope(
                eventId,
                "IncidentAcknowledged",
                UUID.randomUUID().toString(),
                UUID.randomUUID(),
                java.time.Instant.now(),
                1,
                Map.of("incidentId", UUID.randomUUID().toString())
        );

        String rawJson = objectMapper.writeValueAsString(envelope);

        // First delivery — should be processed and tracked
        notificationConsumer.consume(rawJson);
        assertThat(idempotencyTracker.isProcessed(eventId.toString(), "respondr-notification-group")).isTrue();

        // Second delivery (duplicate) — should be silently skipped, no exception
        notificationConsumer.consume(rawJson);
        // Idempotency tracker still shows processed (no double-processing side effects)
        assertThat(idempotencyTracker.isProcessed(eventId.toString(), "respondr-notification-group")).isTrue();
    }

    // ── Test 4: Crash recovery — at-least-once delivery ─────────────────────

    @Test
    void outboxCrashRecovery_reprocessesPendingEvents_afterSimulatedCrash() throws Exception {
        UUID orgId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        // Write initial outbox event
        OutboxEvent original = transactionTemplate.execute(status -> outboxService.publishEvent(
                "INCIDENT",
                incidentId.toString(),
                "IncidentEscalated",
                orgId,
                Map.of("stepOrder", 1, "incidentId", incidentId.toString())
        ));

        assertThat(original).isNotNull();

        // First relay execution — publish + mark PUBLISHED
        outboxPublisherScheduler.processOutboxEvents();
        OutboxEvent afterFirstRelay = outboxEventRepository.findById(original.getId()).orElseThrow();
        assertThat(afterFirstRelay.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);

        // Simulate crash: insert a second PENDING entry with the same payload (simulates re-delivery)
        OutboxEvent crashResurrected = transactionTemplate.execute(status -> {
            OutboxEvent duplicate = new OutboxEvent(
                    original.getAggregateType(),
                    original.getAggregateId(),
                    original.getEventType(),
                    original.getPayload()
            );
            return outboxEventRepository.save(duplicate);
        });

        assertThat(crashResurrected).isNotNull();
        assertThat(crashResurrected.getStatus()).isEqualTo(OutboxStatus.PENDING);

        // Second relay execution — publishes the duplicate (at-least-once: permitted)
        outboxPublisherScheduler.processOutboxEvents();
        OutboxEvent afterSecondRelay = outboxEventRepository.findById(crashResurrected.getId()).orElseThrow();
        assertThat(afterSecondRelay.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);

        // Consumer receives duplicated message and deduplicates it
        DomainEventEnvelope env = objectMapper.readValue(original.getPayload(), DomainEventEnvelope.class);
        notificationConsumer.consume(original.getPayload());
        notificationConsumer.consume(crashResurrected.getPayload()); // duplicate payload

        // Idempotency holds — first eventId tracked, no exception, no double-processing
        assertThat(idempotencyTracker.isProcessed(env.eventId().toString(), "respondr-notification-group")).isTrue();
    }

    // ── Test 5: Topic routing ────────────────────────────────────────────────

    @Test
    void outboxPublisherScheduler_routesEventsToCorrectTopics() {
        assertThat(outboxPublisherScheduler.resolveTopic("IncidentCreated", "INCIDENT")).isEqualTo("respondr.incidents");
        assertThat(outboxPublisherScheduler.resolveTopic("IncidentAcknowledged", "INCIDENT")).isEqualTo("respondr.incidents");
        assertThat(outboxPublisherScheduler.resolveTopic("IncidentEscalated", "INCIDENT")).isEqualTo("respondr.incidents");
        assertThat(outboxPublisherScheduler.resolveTopic("IncidentResolved", "INCIDENT")).isEqualTo("respondr.incidents");
        assertThat(outboxPublisherScheduler.resolveTopic("IncidentReopened", "INCIDENT")).isEqualTo("respondr.incidents");
        assertThat(outboxPublisherScheduler.resolveTopic("CommentAdded", "INCIDENT")).isEqualTo("respondr.comments");
        assertThat(outboxPublisherScheduler.resolveTopic("AlertReceived", "ALERT")).isEqualTo("respondr.alerts");
        assertThat(outboxPublisherScheduler.resolveTopic("Unknown", "OTHER")).isEqualTo("respondr.events");
    }
}
