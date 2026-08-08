package com.respondr.organization;

import com.respondr.common.persistence.BaseEntity;
import jakarta.persistence.*;

/**
 * Top-level tenant entity. Every resource in Respondr belongs to an Organization.
 * The slug is an immutable, URL-safe identifier set at creation time.
 */
@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    // ── Constructors ────────────────────────────────────────────────────────

    protected Organization() {}

    public Organization(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getName() { return name; }
    public String getSlug() { return slug; }

    public void setName(String name) { this.name = name; }
}
