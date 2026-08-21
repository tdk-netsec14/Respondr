package com.respondr.integration;

import com.respondr.incident.Incident;
import com.respondr.incident.IncidentSeverity;
import com.respondr.organization.Organization;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Deduplicated raw alert received via webhook integration.
 */
@Entity
@Table(
    name = "alerts",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_alert_integration_fingerprint",
        columnNames = {"integration_id", "fingerprint"}
    )
)
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integration_id", nullable = false)
    private AlertIntegration integration;

    @Column(name = "external_alert_id")
    private String externalAlertId;

    @Column(nullable = false)
    private String fingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IncidentSeverity severity = IncidentSeverity.HIGH;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // ── Constructors ────────────────────────────────────────────────────────

    protected Alert() {}

    public Alert(Organization organization, AlertIntegration integration, String externalAlertId, String fingerprint, IncidentSeverity severity, String payload) {
        this.organization    = organization;
        this.integration     = integration;
        this.externalAlertId = externalAlertId;
        this.fingerprint     = fingerprint;
        this.severity        = severity != null ? severity : IncidentSeverity.HIGH;
        this.payload         = payload;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public UUID getId()                        { return id; }
    public Organization getOrganization()      { return organization; }
    public AlertIntegration getIntegration()   { return integration; }
    public String getExternalAlertId()         { return externalAlertId; }
    public String getFingerprint()             { return fingerprint; }
    public IncidentSeverity getSeverity()      { return severity; }
    public String getPayload()                 { return payload; }
    public Incident getIncident()              { return incident; }
    public OffsetDateTime getCreatedAt()       { return createdAt; }

    public void setIncident(Incident incident) { this.incident = incident; }
}
