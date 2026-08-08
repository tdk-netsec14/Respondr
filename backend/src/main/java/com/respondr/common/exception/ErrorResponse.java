package com.respondr.common.exception;

import java.time.OffsetDateTime;

/**
 * Uniform error response body returned by all API error handlers.
 *
 * <pre>
 * {
 *   "timestamp":  "2026-09-26T14:00:00Z",
 *   "status":     404,
 *   "code":       "ORGANIZATION_NOT_FOUND",
 *   "message":    "Organization not found: acme",
 *   "path":       "/api/v1/organizations/acme",
 *   "traceId":    "3fa85f64-5717-4562-b3fc-2c963f66afa6"
 * }
 * </pre>
 */
public record ErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String code,
        String message,
        String path,
        String traceId
) {
    public static ErrorResponse of(int status, ErrorCode code, String message, String path, String traceId) {
        return new ErrorResponse(OffsetDateTime.now(), status, code.name(), message, path, traceId);
    }

    public static ErrorResponse of(int status, String code, String message, String path, String traceId) {
        return new ErrorResponse(OffsetDateTime.now(), status, code, message, path, traceId);
    }
}
