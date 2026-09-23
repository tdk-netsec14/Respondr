-- =============================================================================
-- Respondr Phase 6 — Escalation Policies & Steps Schema
-- =============================================================================

-- ─── Escalation Policies ──────────────────────────────────────────────────────
CREATE TABLE escalation_policies (
    id          UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id      UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    team_id     UUID         NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_escalation_policy_org_team ON escalation_policies(org_id, team_id);

-- ─── Escalation Steps ─────────────────────────────────────────────────────────
CREATE TABLE escalation_steps (
    id                UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    policy_id         UUID         NOT NULL REFERENCES escalation_policies(id) ON DELETE CASCADE,
    step_order        INT          NOT NULL,
    delay_seconds     INT          NOT NULL DEFAULT 0,
    target_type       VARCHAR(50)  NOT NULL DEFAULT 'ON_CALL_USER',
    target_reference  UUID,
    created_at        TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_escalation_step_policy_order UNIQUE (policy_id, step_order)
);

CREATE INDEX idx_escalation_step_policy ON escalation_steps(policy_id, step_order ASC);

-- ─── Incident Escalation Execution Tracking ────────────────────────────────────
CREATE TABLE incident_escalation_states (
    id                 UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    incident_id        UUID         NOT NULL REFERENCES incidents(id) ON DELETE CASCADE UNIQUE,
    policy_id          UUID         NOT NULL REFERENCES escalation_policies(id) ON DELETE CASCADE,
    current_step_order INT          NOT NULL DEFAULT 0,
    next_execution_at  TIMESTAMP WITH TIME ZONE,
    status             VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    created_at         TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_incident_escalation_next_exec ON incident_escalation_states(status, next_execution_at ASC);
