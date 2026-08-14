package com.respondr.auth;

import com.respondr.auth.dto.AuthResponse;
import com.respondr.auth.dto.LoginRequest;
import com.respondr.auth.dto.RegisterRequest;
import com.respondr.common.exception.ApiException;
import com.respondr.common.exception.ConflictException;
import com.respondr.common.security.JwtService;
import com.respondr.organization.MembershipRepository;
import com.respondr.organization.OrganizationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                organizationRepository,
                membershipRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void login_throwsUnauthorized_whenUserNotFound() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("missing@test.com", "password")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void login_throwsUnauthorized_whenPasswordMismatch() {
        User user = new User("user@test.com", "hash", "User");
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("user@test.com", "wrong")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void register_throwsConflict_whenEmailExists() {
        when(userRepository.existsByEmail("existing@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("existing@test.com", "password", "Name", "Org", "org-slug")))
                .isInstanceOf(ConflictException.class);
    }
}
