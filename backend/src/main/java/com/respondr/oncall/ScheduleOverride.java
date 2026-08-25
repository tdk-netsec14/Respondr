package com.respondr.oncall;

import com.respondr.auth.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Temporary on-call override replacing the scheduled responder for a specific interval.
 */
@Entity
@Table(name = "schedule_overrides")
public class ScheduleOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private OnCallSchedule schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ── Constructors ────────────────────────────────────────────────────────

    protected ScheduleOverride() {}

    public ScheduleOverride(OnCallSchedule schedule, User user, OffsetDateTime startAt, OffsetDateTime endAt, String reason) {
        this.schedule = schedule;
        this.user     = user;
        this.startAt  = startAt;
        this.endAt    = endAt;
        this.reason   = reason;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public UUID getId()                  { return id; }
    public OnCallSchedule getSchedule()  { return schedule; }
    public User getUser()                { return user; }
    public OffsetDateTime getStartAt()   { return startAt; }
    public OffsetDateTime getEndAt()     { return endAt; }
    public String getReason()            { return reason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
