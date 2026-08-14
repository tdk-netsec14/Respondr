package com.respondr.common.security;

import com.respondr.organization.MemberRole;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filter that parses Bearer JWTs, sets Spring Security authentication,
 * and initializes request-scoped {@link TenantContextHolder}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();

            if (jwtService.validateToken(token)) {
                Claims claims = jwtService.parseClaims(token);
                String tokenType = jwtService.extractTokenType(claims);

                if ("ACCESS".equals(tokenType)) {
                    UUID userId = jwtService.extractUserId(claims);
                    UUID orgId = jwtService.extractOrgId(claims);
                    MemberRole role = jwtService.extractRole(claims);
                    String email = claims.get("email", String.class);

                    TenantContextHolder.TenantContext context =
                            new TenantContextHolder.TenantContext(orgId, userId, role, email);
                    TenantContextHolder.setContext(context);

                    SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role.name());
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(context, null, List.of(authority));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
