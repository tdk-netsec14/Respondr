package com.respondr.event;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Transactional outbox pattern entity.
 * Written in the same DB transaction as domain state mutations,
 * then published asynchronously to Kafka by background relay.
 */
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 255)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private OutboxStatus status = OutboxStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    protected OutboxEvent() {}

    public OutboxEvent(String aggregateType, String aggregateId, String eventType, String payload) {
        this.aggregateType = aggregateType;
        this.aggregateId   = aggregateId;
        this.eventType     = eventType;
        this.payload       = payload;
    }

    public UUID getId()                    { return id; }
    public String getAggregateType()       { return aggregateType; }
    public String getAggregateId()         { return aggregateId; }
    public String getEventType()           { return eventType; }
    public String getPayload()             { return payload; }
    public OutboxStatus getStatus()        { return status; }
    public OffsetDateTime getCreatedAt()   { return createdAt; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }

    public void markPublished() {
        this.status      = OutboxStatus.PUBLISHED;
        this.publishedAt = OffsetDateTime.now();
    }

    public void markFailed() {
        this.status = OutboxStatus.FAILED;
    }
}
