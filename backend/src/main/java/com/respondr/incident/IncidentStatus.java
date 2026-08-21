package com.respondr.incident;

/** Lifecycle status of an incident. */
public enum IncidentStatus {
    OPEN,
    ACKNOWLEDGED,
    INVESTIGATING,
    RESOLVED,
    CANCELLED,
    REOPENED,
    CLOSED
}
