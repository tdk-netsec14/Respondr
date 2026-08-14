package com.respondr.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Centralised exception → HTTP response mapping.
 * All errors follow the {@link ErrorResponse} contract:
 * {@code {timestamp, status, code, message, path, traceId}}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String TRACE_ID_KEY = "traceId";

    // ── Domain / API exceptions ───────────────────────────────────────────────

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest req) {
        log.warn("API exception [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(buildError(ex.getHttpStatus().value(), ex.getErrorCode(), ex.getMessage(), req));
    }

    // ── Security exceptions ───────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        log.warn("Access denied on path {}: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(buildError(HttpStatus.FORBIDDEN.value(), ErrorCode.ACCESS_DENIED,
                        "Access denied: insufficient permissions or cross-tenant operation attempted", req));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest req) {
        log.warn("Authentication failed on path {}: {}", req.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(buildError(HttpStatus.UNAUTHORIZED.value(), ErrorCode.UNAUTHORIZED,
                        "Authentication required", req));
    }

    // ── Bean-validation failures (@Valid) ─────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        String message = "Validation failed: " + details;
        log.debug("Validation error: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(buildError(HttpStatus.BAD_REQUEST.value(), ErrorCode.VALIDATION_ERROR, message, req));
    }

    // ── Database constraint violations ────────────────────────────────────────

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest req) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(buildError(HttpStatus.CONFLICT.value(), ErrorCode.CONFLICT,
                        "A database constraint was violated. Check for duplicate values.", req));
    }

    // ── Catch-all ─────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(buildError(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        ErrorCode.INTERNAL_ERROR, "An unexpected error occurred.", req));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private ErrorResponse buildError(int status, ErrorCode code, String message, HttpServletRequest req) {
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        return ErrorResponse.of(status, code, message, req.getRequestURI(), traceId);
    }
}
