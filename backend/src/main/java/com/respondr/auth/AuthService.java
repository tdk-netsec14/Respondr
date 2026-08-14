package com.respondr.auth;

import com.respondr.auth.dto.*;
import com.respondr.common.exception.ApiException;
import com.respondr.common.exception.ConflictException;
import com.respondr.common.exception.ErrorCode;
import com.respondr.common.security.JwtService;
import com.respondr.organization.*;
import com.respondr.organization.dto.OrganizationResponse;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            MembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, "Invalid email or password"));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, "Invalid email or password");
        }

        List<Membership> memberships = membershipRepository.findByOrganizationId(user.getId());
        Membership membership;
        if (!memberships.isEmpty()) {
            membership = memberships.get(0);
        } else {
            // Find any membership by user
            membership = membershipRepository.findAll().stream()
                    .filter(m -> m.getUser().getId().equals(user.getId()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "User has no active organization membership"));
        }

        Organization org = membership.getOrganization();
        MemberRole role = membership.getRole();

        String accessToken = jwtService.generateAccessToken(user, org.getId(), role);
        String refreshToken = jwtService.generateRefreshToken(user, org.getId());

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs(),
                UserDto.from(user),
                OrganizationResponse.from(org)
        );
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ConflictException(ErrorCode.USER_EMAIL_TAKEN, "User with email already exists: " + req.email());
        }

        if (organizationRepository.existsBySlug(req.orgSlug())) {
            throw ConflictException.slugTaken(req.orgSlug());
        }

        String passwordHash = passwordEncoder.encode(req.password());
        User user = userRepository.save(new User(req.email(), passwordHash, req.name()));
        Organization org = organizationRepository.save(new Organization(req.orgName(), req.orgSlug()));
        Membership membership = membershipRepository.save(new Membership(org, user, MemberRole.OWNER));

        String accessToken = jwtService.generateAccessToken(user, org.getId(), MemberRole.OWNER);
        String refreshToken = jwtService.generateRefreshToken(user, org.getId());

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtService.getAccessTokenExpirationMs(),
                UserDto.from(user),
                OrganizationResponse.from(org)
        );
    }

    public AuthResponse refresh(RefreshTokenRequest req) {
        if (!jwtService.validateToken(req.refreshToken())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_TOKEN, "Invalid or expired refresh token");
        }

        Claims claims = jwtService.parseClaims(req.refreshToken());
        if (!"REFRESH".equals(jwtService.extractTokenType(claims))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_TOKEN, "Token is not a refresh token");
        }

        UUID userId = jwtService.extractUserId(claims);
        UUID orgId = jwtService.extractOrgId(claims);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_TOKEN, "User no longer exists"));

        Membership membership = membershipRepository.findByOrganizationIdAndUserId(orgId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "Membership no longer active"));

        Organization org = membership.getOrganization();
        MemberRole role = membership.getRole();

        String newAccessToken = jwtService.generateAccessToken(user, org.getId(), role);
        String newRefreshToken = jwtService.generateRefreshToken(user, org.getId());

        return AuthResponse.of(
                newAccessToken,
                newRefreshToken,
                jwtService.getAccessTokenExpirationMs(),
                UserDto.from(user),
                OrganizationResponse.from(org)
        );
    }
}
