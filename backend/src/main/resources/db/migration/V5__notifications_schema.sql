-- =============================================================================
-- Respondr Phase 8 — Notifications Schema
-- =============================================================================

CREATE TABLE notifications (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id          UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    incident_id     UUID         REFERENCES incidents(id) ON DELETE SET NULL,
    recipient       VARCHAR(255) NOT NULL,
    channel         VARCHAR(50)  NOT NULL,
    status          VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    attempts        INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    error_message   TEXT
);

CREATE INDEX idx_notification_org_created ON notifications(org_id, created_at DESC);
CREATE INDEX idx_notification_status_created ON notifications(status, created_at ASC);
