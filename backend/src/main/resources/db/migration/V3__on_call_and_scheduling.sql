-- =============================================================================
-- Respondr Phase 5 — On-Call & Scheduling Schema
-- =============================================================================

-- ─── On-Call Schedules ────────────────────────────────────────────────────────
CREATE TABLE on_call_schedules (
    id              UUID         DEFAULT gen_random_uuid() PRIMARY KEY,
    org_id          UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    team_id         UUID         NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    timezone        VARCHAR(100) NOT NULL DEFAULT 'UTC',
    rotation_rules  JSONB        NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_on_call_schedule_org_team ON on_call_schedules(org_id, team_id);

-- ─── Schedule Overrides ───────────────────────────────────────────────────────
CREATE TABLE schedule_overrides (
    id           UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    schedule_id  UUID        NOT NULL REFERENCES on_call_schedules(id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at       TIMESTAMP WITH TIME ZONE NOT NULL,
    reason       TEXT,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_override_dates CHECK (end_at > start_at)
);

CREATE INDEX idx_schedule_override_schedule_time ON schedule_overrides(schedule_id, start_at, end_at);
