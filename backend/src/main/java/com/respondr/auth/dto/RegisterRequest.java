package com.respondr.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "email must not be blank")
        @Email(message = "email must be valid")
        String email,

        @NotBlank(message = "password must not be blank")
        @Size(min = 6, message = "password must be at least 6 characters")
        String password,

        @NotBlank(message = "name must not be blank")
        String name,

        @NotBlank(message = "orgName must not be blank")
        String orgName,

        @NotBlank(message = "orgSlug must not be blank")
        @Pattern(
                regexp = "^[a-z0-9][a-z0-9-]{1,98}[a-z0-9]$",
                message = "slug must be 3-100 lowercase alphanumeric characters or hyphens"
        )
        String orgSlug
) {}
