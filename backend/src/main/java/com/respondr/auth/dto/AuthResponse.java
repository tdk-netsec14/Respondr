package com.respondr.auth.dto;

import com.respondr.organization.dto.OrganizationResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserDto user,
        OrganizationResponse organization
) {
    public static AuthResponse of(
            String accessToken,
            String refreshToken,
            long expiresIn,
            UserDto user,
            OrganizationResponse organization) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresIn / 1000,
                user,
                organization
        );
    }
}
