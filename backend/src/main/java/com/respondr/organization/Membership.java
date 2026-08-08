package com.respondr.organization;

import com.respondr.auth.User;
import com.respondr.common.persistence.BaseEntity;
import jakarta.persistence.*;

/**
 * Junction entity linking a User to an Organization with a specific role.
 * The (org_id, user_id) combination is unique, enforced by both a DB constraint
 * and the {@code uq_membership_org_user} Flyway constraint.
 */
@Entity
@Table(
    name = "memberships",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_membership_org_user",
        columnNames = {"org_id", "user_id"}
    )
)
public class Membership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MemberStatus status = MemberStatus.ACTIVE;

    // ── Constructors ────────────────────────────────────────────────────────

    protected Membership() {}

    public Membership(Organization organization, User user, MemberRole role) {
        this.organization = organization;
        this.user         = user;
        this.role         = role;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public Organization getOrganization() { return organization; }
    public User getUser()                 { return user; }
    public MemberRole getRole()           { return role; }
    public MemberStatus getStatus()       { return status; }

    public void setRole(MemberRole role)     { this.role = role; }
    public void setStatus(MemberStatus s)    { this.status = s; }
}
