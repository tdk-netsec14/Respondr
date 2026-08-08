package com.respondr.servicecatalog;

import com.respondr.common.persistence.BaseEntity;
import com.respondr.organization.Organization;
import com.respondr.team.Team;
import jakarta.persistence.*;

/**
 * A monitored service registered in the Respondr catalog.
 * The {@code key} is a short, URL-safe identifier unique within the organization
 * (e.g. "payments-api").
 *
 * Named {@code ServiceEntity} to avoid clashing with {@code org.springframework.stereotype.Service}.
 */
@Entity
@Table(
    name = "services",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_service_org_key",
        columnNames = {"org_id", "key"}
    )
)
public class ServiceEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(nullable = false)
    private String name;

    @Column(name = "key", nullable = false, length = 100)
    private String key;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    // ── Constructors ────────────────────────────────────────────────────────

    protected ServiceEntity() {}

    public ServiceEntity(Organization organization, Team team, String name, String key, String description) {
        this.organization = organization;
        this.team         = team;
        this.name         = name;
        this.key          = key;
        this.description  = description;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public Organization getOrganization() { return organization; }
    public Team getTeam()                 { return team; }
    public String getName()               { return name; }
    public String getKey()                { return key; }
    public String getDescription()        { return description; }
    public boolean isActive()             { return active; }

    public void setName(String name)             { this.name = name; }
    public void setDescription(String d)         { this.description = d; }
    public void setActive(boolean active)        { this.active = active; }
    public void setTeam(Team team)               { this.team = team; }
}
