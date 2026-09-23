package com.respondr.notification;

import com.respondr.common.persistence.BaseEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Persisted notification tracking entity.
 * Represents an outbound notification attempt (in-app or email) triggered by domain events.
 */
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

    @Column(name = "org_id", nullable = false)
    private UUID orgId;

    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(nullable = false)
    private int attempts = 0;

    @Column(name = "last_attempt_at")
    private OffsetDateTime lastAttemptAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // ── Constructors ────────────────────────────────────────────────────────

    protected Notification() {}

    public Notification(UUID orgId, UUID incidentId, String recipient, NotificationChannel channel) {
        this.orgId      = orgId;
        this.incidentId = incidentId;
        this.recipient  = recipient;
        this.channel    = channel;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public UUID getOrgId()                 { return orgId; }
    public UUID getIncidentId()            { return incidentId; }
    public String getRecipient()           { return recipient; }
    public NotificationChannel getChannel() { return channel; }
    public NotificationStatus getStatus()  { return status; }
    public int getAttempts()               { return attempts; }
    public OffsetDateTime getLastAttemptAt() { return lastAttemptAt; }
    public String getErrorMessage()        { return errorMessage; }

    public void incrementAttempts() {
        this.attempts++;
        this.lastAttemptAt = OffsetDateTime.now();
    }

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.errorMessage = null;
    }

    public void markFailed(String error) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = error;
    }

    public void markDeadLetter(String error) {
        this.status = NotificationStatus.DEAD_LETTER;
        this.errorMessage = error;
    }
}
