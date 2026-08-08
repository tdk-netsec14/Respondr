package com.respondr.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for all domain exceptions that map to an HTTP error response.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final ErrorCode  errorCode;

    public ApiException(HttpStatus httpStatus, ErrorCode errorCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode  = errorCode;
    }

    public ApiException(HttpStatus httpStatus, ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode  = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
