package com.respondr.common.security;

import com.respondr.common.exception.ApiException;
import com.respondr.organization.MemberRole;
import org.springframework.http.HttpStatus;
import com.respondr.common.exception.ErrorCode;

import java.util.Optional;
import java.util.UUID;

/**
 * Thread-local holder for request-scoped tenant and user context.
 * Populated by {@link JwtAuthenticationFilter} for every authenticated request.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new ThreadLocal<>();

    private TenantContextHolder() {}

    public static void setContext(TenantContext context) {
        CONTEXT.set(context);
    }

    public static Optional<TenantContext> getContext() {
        return Optional.ofNullable(CONTEXT.get());
    }

    public static TenantContext getRequiredContext() {
        return getContext().orElseThrow(() ->
                new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.NOT_FOUND, "No authenticated tenant context found"));
    }

    public static UUID getRequiredOrgId() {
        return getRequiredContext().orgId();
    }

    public static UUID getRequiredUserId() {
        return getRequiredContext().userId();
    }

    public static void clear() {
        CONTEXT.remove();
    }

    public record TenantContext(
            UUID orgId,
            UUID userId,
            MemberRole role,
            String email
    ) {}
}
