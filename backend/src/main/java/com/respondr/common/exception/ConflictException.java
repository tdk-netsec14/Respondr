package com.respondr.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when an operation would violate a uniqueness constraint. Maps to HTTP 409.
 */
public class ConflictException extends ApiException {

    public ConflictException(ErrorCode code, String message) {
        super(HttpStatus.CONFLICT, code, message);
    }

    public static ConflictException slugTaken(String slug) {
        return new ConflictException(
                ErrorCode.ORGANIZATION_SLUG_TAKEN,
                "Organization slug already exists: " + slug);
    }

    public static ConflictException serviceKeyTaken(String key) {
        return new ConflictException(
                ErrorCode.SERVICE_KEY_TAKEN,
                "Service key already exists in this organization: " + key);
    }
}
