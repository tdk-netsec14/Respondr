package com.respondr.incident;

import com.respondr.auth.User;
import com.respondr.common.persistence.BaseEntity;
import jakarta.persistence.*;

/**
 * Collaborative comment on an incident timeline.
 */
@Entity
@Table(name = "comments")
public class Comment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    // ── Constructors ────────────────────────────────────────────────────────

    protected Comment() {}

    public Comment(Incident incident, User author, String body) {
        this.incident = incident;
        this.author   = author;
        this.body     = body;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public Incident getIncident() { return incident; }
    public User getAuthor()       { return author; }
    public String getBody()       { return body; }

    public void setBody(String body) { this.body = body; }
}
