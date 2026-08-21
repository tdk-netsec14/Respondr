package com.respondr.integration;

import com.respondr.common.persistence.BaseEntity;
import com.respondr.organization.Organization;
import com.respondr.servicecatalog.ServiceEntity;
import jakarta.persistence.*;

/**
 * Represents a webhook alert integration (e.g. Prometheus, Datadog, PagerDuty, Generic).
 */
@Entity
@Table(
    name = "alert_integrations",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_alert_integration_org_key",
        columnNames = {"org_id", "key"}
    )
)
public class AlertIntegration extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceEntity service;

    @Column(nullable = false)
    private String name;

    @Column(name = "key", nullable = false, length = 100)
    private String key;

    @Column(nullable = false, length = 50)
    private String provider = "GENERIC";

    @Column(name = "secret_key", nullable = false)
    private String secretKey;

    @Column(nullable = false)
    private boolean active = true;

    // ── Constructors ────────────────────────────────────────────────────────

    protected AlertIntegration() {}

    public AlertIntegration(Organization organization, ServiceEntity service, String name, String key, String provider, String secretKey) {
        this.organization = organization;
        this.service      = service;
        this.name         = name;
        this.key          = key;
        this.provider     = provider != null ? provider.toUpperCase() : "GENERIC";
        this.secretKey    = secretKey;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public Organization getOrganization() { return organization; }
    public ServiceEntity getService()     { return service; }
    public String getName()               { return name; }
    public String getKey()                { return key; }
    public String getProvider()           { return provider; }
    public String getSecretKey()          { return secretKey; }
    public boolean isActive()             { return active; }

    public void setName(String name)           { this.name = name; }
    public void setService(ServiceEntity service) { this.service = service; }
    public void setActive(boolean active)      { this.active = active; }
    public void setSecretKey(String s)         { this.secretKey = s; }
}
