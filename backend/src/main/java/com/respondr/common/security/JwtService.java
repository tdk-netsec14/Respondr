package com.respondr.common.security;

import com.respondr.auth.User;
import com.respondr.organization.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

/**
 * Service for issuing and validating JWT access and refresh tokens.
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtService(
            @Value("${respondr.jwt.secret}") String secret,
            @Value("${respondr.jwt.access-token-expiration-ms:900000}") long accessTokenExpirationMs,
            @Value("${respondr.jwt.refresh-token-expiration-ms:604800000}") long refreshTokenExpirationMs) {
        byte[] keyBytes = Decoders.BASE64.decode(secret.length() % 2 != 0 ? secret + "=" : secret);
        if (keyBytes.length < 32) {
            keyBytes = secret.getBytes();
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(User user, UUID orgId, MemberRole role) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("orgId", orgId.toString())
                .claim("role", role.name())
                .claim("type", "ACCESS")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(User user, UUID orgId) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("orgId", orgId.toString())
                .claim("type", "REFRESH")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public UUID extractOrgId(Claims claims) {
        return UUID.fromString(claims.get("orgId", String.class));
    }

    public MemberRole extractRole(Claims claims) {
        String roleStr = claims.get("role", String.class);
        return roleStr != null ? MemberRole.valueOf(roleStr) : MemberRole.VIEWER;
    }

    public String extractTokenType(Claims claims) {
        return claims.get("type", String.class);
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}
