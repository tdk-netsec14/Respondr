package com.respondr.escalation;

import com.respondr.common.persistence.BaseEntity;
import com.respondr.organization.Organization;
import com.respondr.team.Team;
import jakarta.persistence.*;

/**
 * Defines an escalation policy owned by an organization and team.
 */
@Entity
@Table(name = "escalation_policies")
public class EscalationPolicy extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    protected EscalationPolicy() {}

    public EscalationPolicy(Organization organization, Team team, String name) {
        this.organization = organization;
        this.team = team;
        this.name = name;
    }

    public Organization getOrganization() { return organization; }
    public Team getTeam()                 { return team; }
    public String getName()               { return name; }
    public boolean isActive()             { return active; }

    public void setName(String name)        { this.name = name; }
    public void setActive(boolean active)  { this.active = active; }
}
