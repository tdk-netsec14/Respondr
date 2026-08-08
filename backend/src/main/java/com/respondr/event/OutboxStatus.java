package com.respondr.event;

/** Publication lifecycle status of an outbox event. */
public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
