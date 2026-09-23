-- =============================================================================
-- Respondr Phase 1 — Initial Schema
-- =============================================================================

-- Enable pgcrypto for gen_random_uuid() (available in PostgreSQL 13+ natively)
-- gen_random_uuid() is a built-in function in PostgreSQL 13+

-- ─── Organizations ────────────────────────────────────────────────────────────
CREATE TABLE organizations (
    id          UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_organization_slug UNIQUE (slug)
);

-- ─── Users ────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id             UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL,
    name           VARCHAR(255) NOT NULL,
    status         VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at     TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_email UNIQUE (email)
);

-- ─── Memberships ──────────────────────────────────────────────────────────────
CREATE TABLE memberships (
    id          UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id      UUID        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role        VARCHAR(50) NOT NULL,
    status      VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_membership_org_user UNIQUE (org_id, user_id)
);

CREATE INDEX idx_membership_org  ON memberships(org_id);
CREATE INDEX idx_membership_user ON memberships(user_id);

-- ─── Teams ────────────────────────────────────────────────────────────────────
CREATE TABLE teams (
    id           UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id       UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    description  TEXT,
    created_at   TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_team_org ON teams(org_id);

-- ─── Services ─────────────────────────────────────────────────────────────────
CREATE TABLE services (
    id           UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id       UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    team_id      UUID         REFERENCES teams(id) ON DELETE SET NULL,
    name         VARCHAR(255) NOT NULL,
    "key"        VARCHAR(100) NOT NULL,
    description  TEXT,
    active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_service_org_key UNIQUE (org_id, "key")
);

CREATE INDEX idx_service_org  ON services(org_id);
CREATE INDEX idx_service_team ON services(team_id);

-- ─── Environments ─────────────────────────────────────────────────────────────
CREATE TABLE environments (
    id          UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    service_id  UUID        NOT NULL REFERENCES services(id) ON DELETE CASCADE,
    name        VARCHAR(100) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_environment_service ON environments(service_id);

-- ─── Incidents ────────────────────────────────────────────────────────────────
CREATE TABLE incidents (
    id           UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id       UUID         NOT NULL REFERENCES organizations(id),
    service_id   UUID         REFERENCES services(id) ON DELETE SET NULL,
    team_id      UUID         REFERENCES teams(id) ON DELETE SET NULL,
    title        VARCHAR(500) NOT NULL,
    description  TEXT,
    severity     VARCHAR(50)  NOT NULL,
    status       VARCHAR(50)  NOT NULL DEFAULT 'OPEN',
    source       VARCHAR(100),
    assignee_id  UUID         REFERENCES users(id) ON DELETE SET NULL,
    created_at   TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    resolved_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_incident_org_status_created   ON incidents(org_id, status, created_at DESC);
CREATE INDEX idx_incident_org_service_created  ON incidents(org_id, service_id, created_at DESC);
CREATE INDEX idx_incident_org_severity_created ON incidents(org_id, severity, created_at DESC);

-- ─── Incident Events ──────────────────────────────────────────────────────────
CREATE TABLE incident_events (
    id           UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    incident_id  UUID         NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    event_type   VARCHAR(100) NOT NULL,
    actor_id     UUID         REFERENCES users(id) ON DELETE SET NULL,
    metadata     JSONB,
    created_at   TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incident_event_incident_created ON incident_events(incident_id, created_at DESC);

-- ─── Comments ─────────────────────────────────────────────────────────────────
CREATE TABLE comments (
    id           UUID  DEFAULT gen_random_uuid() PRIMARY KEY,
    incident_id  UUID  NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
    author_id    UUID  NOT NULL REFERENCES users(id),
    body         TEXT  NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_comment_incident ON comments(incident_id);

-- ─── Audit Logs ───────────────────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id           UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id       UUID         NOT NULL,
    actor        VARCHAR(255),
    action       VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(100) NOT NULL,
    entity_id    VARCHAR(255),
    metadata     JSONB,
    created_at   TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_org_created ON audit_logs(org_id, created_at DESC);

-- ─── Outbox Events ────────────────────────────────────────────────────────────
CREATE TABLE outbox_events (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    aggregate_type  VARCHAR(100) NOT NULL,
    aggregate_id    VARCHAR(255) NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    payload         JSONB        NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    created_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    published_at    TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_event_status_created ON outbox_events(status, created_at ASC);
