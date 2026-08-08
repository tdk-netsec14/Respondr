package com.respondr.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a requested resource does not exist. Maps to HTTP 404.
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(ErrorCode code, String message) {
        super(HttpStatus.NOT_FOUND, code, message);
    }

    public static ResourceNotFoundException organization(String identifier) {
        return new ResourceNotFoundException(
                ErrorCode.ORGANIZATION_NOT_FOUND,
                "Organization not found: " + identifier);
    }

    public static ResourceNotFoundException team(String identifier) {
        return new ResourceNotFoundException(
                ErrorCode.TEAM_NOT_FOUND,
                "Team not found: " + identifier);
    }

    public static ResourceNotFoundException service(String identifier) {
        return new ResourceNotFoundException(
                ErrorCode.SERVICE_NOT_FOUND,
                "Service not found: " + identifier);
    }
}
