package com.respondr.servicecatalog;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A deployment environment (e.g. production, staging) associated with a service.
 */
@Entity
@Table(name = "environments")
public class Environment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceEntity service;

    @Column(nullable = false, length = 100)
    private String name;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ── Constructors ────────────────────────────────────────────────────────

    protected Environment() {}

    public Environment(ServiceEntity service, String name) {
        this.service = service;
        this.name    = name;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public UUID getId()              { return id; }
    public ServiceEntity getService() { return service; }
    public String getName()          { return name; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
