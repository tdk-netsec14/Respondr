package com.respondr.incident;

import com.respondr.auth.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable audit entry recording state transitions and actions on an incident.
 * Written by the incident domain on every status change, assignment, or annotation.
 */
@Entity
@Table(name = "incident_events")
public class IncidentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /** JSON payload stored as JSONB in PostgreSQL. */
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ── Constructors ────────────────────────────────────────────────────────

    protected IncidentEvent() {}

    public IncidentEvent(Incident incident, String eventType, User actor, String metadata) {
        this.incident  = incident;
        this.eventType = eventType;
        this.actor     = actor;
        this.metadata  = metadata;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public UUID getId()                  { return id; }
    public Incident getIncident()        { return incident; }
    public String getEventType()         { return eventType; }
    public User getActor()               { return actor; }
    public String getMetadata()          { return metadata; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
