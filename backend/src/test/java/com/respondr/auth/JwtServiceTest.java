package com.respondr.auth;

import com.respondr.common.security.JwtService;
import com.respondr.organization.MemberRole;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900000, 604800000);
    }

    @Test
    void generatesAndValidatesAccessToken() {
        User user = new User("user@acme.com", "hashed", "User Acme");
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        // Inject UUID id using reflection for unit testing
        try {
            var field = com.respondr.common.persistence.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String token = jwtService.generateAccessToken(user, orgId, MemberRole.OWNER);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();

        Claims claims = jwtService.parseClaims(token);
        assertThat(jwtService.extractUserId(claims)).isEqualTo(userId);
        assertThat(jwtService.extractOrgId(claims)).isEqualTo(orgId);
        assertThat(jwtService.extractRole(claims)).isEqualTo(MemberRole.OWNER);
        assertThat(jwtService.extractTokenType(claims)).isEqualTo("ACCESS");
    }

    @Test
    void generatesAndValidatesRefreshToken() {
        User user = new User("user@acme.com", "hashed", "User Acme");
        UUID userId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();

        try {
            var field = com.respondr.common.persistence.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        String refreshToken = jwtService.generateRefreshToken(user, orgId);

        assertThat(refreshToken).isNotBlank();
        assertThat(jwtService.validateToken(refreshToken)).isTrue();

        Claims claims = jwtService.parseClaims(refreshToken);
        assertThat(jwtService.extractUserId(claims)).isEqualTo(userId);
        assertThat(jwtService.extractOrgId(claims)).isEqualTo(orgId);
        assertThat(jwtService.extractTokenType(claims)).isEqualTo("REFRESH");
    }

    @Test
    void returnsFalseForInvalidToken() {
        assertThat(jwtService.validateToken("invalid.jwt.token")).isFalse();
    }
}
