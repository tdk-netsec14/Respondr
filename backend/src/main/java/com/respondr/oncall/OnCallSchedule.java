package com.respondr.oncall;

import com.respondr.common.persistence.BaseEntity;
import com.respondr.organization.Organization;
import com.respondr.team.Team;
import jakarta.persistence.*;

/**
 * On-call schedule entity for a team. Defines timezone and rotation rules.
 */
@Entity
@Table(name = "on_call_schedules")
public class OnCallSchedule extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String timezone = "UTC";

    /** JSON payload containing rotation details (participantUserIds, shiftLengthHours, rotationStartAt). */
    @Column(name = "rotation_rules", nullable = false, columnDefinition = "jsonb")
    private String rotationRules;

    @Column(nullable = false)
    private boolean active = true;

    // ── Constructors ────────────────────────────────────────────────────────

    protected OnCallSchedule() {}

    public OnCallSchedule(Organization organization, Team team, String name, String timezone, String rotationRules) {
        this.organization  = organization;
        this.team          = team;
        this.name          = name;
        this.timezone      = timezone != null ? timezone : "UTC";
        this.rotationRules = rotationRules;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public Organization getOrganization() { return organization; }
    public Team getTeam()                 { return team; }
    public String getName()               { return name; }
    public String getTimezone()           { return timezone; }
    public String getRotationRules()      { return rotationRules; }
    public boolean isActive()             { return active; }

    public void setName(String name)                 { this.name = name; }
    public void setTimezone(String timezone)         { this.timezone = timezone; }
    public void setRotationRules(String rules)       { this.rotationRules = rules; }
    public void setActive(boolean active)            { this.active = active; }
}
