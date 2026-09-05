package com.respondr.common.exception;

/**
 * Machine-readable error codes returned in the error response body.
 * Format: DOMAIN_DESCRIPTION
 */
public enum ErrorCode {

    // ── Generic ───────────────────────────────────────────────────────────────
    INTERNAL_ERROR,
    VALIDATION_ERROR,
    NOT_FOUND,
    CONFLICT,
    BAD_REQUEST,
    UNAUTHORIZED,
    ACCESS_DENIED,

    // ── Auth ──────────────────────────────────────────────────────────────────
    INVALID_CREDENTIALS,
    INVALID_TOKEN,
    USER_EMAIL_TAKEN,

    // ── Organization ─────────────────────────────────────────────────────────
    ORGANIZATION_NOT_FOUND,
    ORGANIZATION_SLUG_TAKEN,

    // ── Team ─────────────────────────────────────────────────────────────────
    TEAM_NOT_FOUND,
    TEAM_NOT_IN_ORGANIZATION,

    // ── Service ──────────────────────────────────────────────────────────────
    SERVICE_NOT_FOUND,
    SERVICE_KEY_TAKEN,
    SERVICE_NOT_IN_ORGANIZATION,

    // ── Escalation & Rate Limiting ────────────────────────────────────────────
    RATE_LIMIT_EXCEEDED,
    ESCALATION_POLICY_NOT_FOUND,
    ESCALATION_STEP_NOT_FOUND,
}
