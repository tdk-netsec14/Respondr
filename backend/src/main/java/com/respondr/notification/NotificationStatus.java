package com.respondr.notification;

/** Lifecycle status of a notification dispatch attempt. */
public enum NotificationStatus {
    PENDING,
    SENT,
    FAILED,
    DEAD_LETTER
}
