package com.respondr.auth;

import com.respondr.common.persistence.BaseEntity;
import jakarta.persistence.*;

/**
 * Represents an authenticated user identity.
 * Credentials (passwordHash) will be managed by Spring Security in Phase 2.
 */
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserStatus status = UserStatus.ACTIVE;

    // ── Constructors ────────────────────────────────────────────────────────

    protected User() {}

    public User(String email, String passwordHash, String name) {
        this.email        = email;
        this.passwordHash = passwordHash;
        this.name         = name;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getEmail()        { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getName()         { return name; }
    public UserStatus getStatus()   { return status; }

    public void setName(String name)              { this.name = name; }
    public void setStatus(UserStatus status)      { this.status = status; }
    public void setPasswordHash(String hash)      { this.passwordHash = hash; }
}
