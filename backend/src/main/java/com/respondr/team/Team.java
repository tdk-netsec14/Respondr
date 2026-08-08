package com.respondr.team;

import com.respondr.common.persistence.BaseEntity;
import com.respondr.organization.Organization;
import jakarta.persistence.*;

/**
 * A team within an organization, used for ownership of services and on-call schedules.
 */
@Entity
@Table(name = "teams")
public class Team extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    // ── Constructors ────────────────────────────────────────────────────────

    protected Team() {}

    public Team(Organization organization, String name, String description) {
        this.organization = organization;
        this.name         = name;
        this.description  = description;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public Organization getOrganization() { return organization; }
    public String getName()               { return name; }
    public String getDescription()        { return description; }

    public void setName(String name)             { this.name = name; }
    public void setDescription(String description) { this.description = description; }
}
