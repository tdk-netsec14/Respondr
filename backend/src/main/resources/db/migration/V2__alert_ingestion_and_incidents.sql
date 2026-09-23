-- =============================================================================
-- Respondr Phase 4 — Alert Ingestion & Incident Lifecycle Schema
-- =============================================================================

-- ─── Alert Integrations ───────────────────────────────────────────────────────
CREATE TABLE alert_integrations (
    id          UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id      UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    service_id  UUID         REFERENCES services(id) ON DELETE SET NULL,
    name        VARCHAR(255) NOT NULL,
    "key"       VARCHAR(100) NOT NULL,
    provider    VARCHAR(50)  NOT NULL DEFAULT 'GENERIC',
    secret_key  VARCHAR(255) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_alert_integration_org_key UNIQUE (org_id, "key")
);

CREATE INDEX idx_alert_integration_org ON alert_integrations(org_id);

-- ─── Alerts ────────────────────────────────────────────────────────────────────
CREATE TABLE alerts (
    id                 UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id             UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    integration_id     UUID         NOT NULL REFERENCES alert_integrations(id) ON DELETE CASCADE,
    external_alert_id  VARCHAR(255),
    fingerprint        VARCHAR(255) NOT NULL,
    severity           VARCHAR(50)  NOT NULL DEFAULT 'HIGH',
    payload            JSONB        NOT NULL,
    incident_id        UUID         REFERENCES incidents(id) ON DELETE SET NULL,
    created_at         TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_alert_integration_fingerprint UNIQUE (integration_id, fingerprint)
);

CREATE INDEX idx_alert_org_created ON alerts(org_id, created_at DESC);
CREATE INDEX idx_alert_incident    ON alerts(incident_id);
