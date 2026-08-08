package com.respondr.incident;

import com.respondr.auth.User;
import com.respondr.common.persistence.BaseEntity;
import com.respondr.organization.Organization;
import com.respondr.servicecatalog.ServiceEntity;
import com.respondr.team.Team;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Core incident entity. Created automatically by the alert pipeline (Phase 3)
 * or manually by responders. Tracks full lifecycle from OPEN to CLOSED.
 */
@Entity
@Table(name = "incidents")
public class Incident extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ServiceEntity service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(length = 100)
    private String source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    // ── Constructors ────────────────────────────────────────────────────────

    protected Incident() {}

    public Incident(Organization organization, String title, IncidentSeverity severity) {
        this.organization = organization;
        this.title        = title;
        this.severity     = severity;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public Organization getOrganization()    { return organization; }
    public ServiceEntity getService()        { return service; }
    public Team getTeam()                    { return team; }
    public String getTitle()                 { return title; }
    public String getDescription()           { return description; }
    public IncidentSeverity getSeverity()    { return severity; }
    public IncidentStatus getStatus()        { return status; }
    public String getSource()                { return source; }
    public User getAssignee()                { return assignee; }
    public OffsetDateTime getResolvedAt()    { return resolvedAt; }

    public void setTitle(String title)              { this.title = title; }
    public void setDescription(String description)  { this.description = description; }
    public void setSeverity(IncidentSeverity s)     { this.severity = s; }
    public void setStatus(IncidentStatus s)         { this.status = s; }
    public void setSource(String source)            { this.source = source; }
    public void setAssignee(User assignee)          { this.assignee = assignee; }
    public void setService(ServiceEntity service)   { this.service = service; }
    public void setTeam(Team team)                  { this.team = team; }
    public void setResolvedAt(OffsetDateTime t)     { this.resolvedAt = t; }
}
