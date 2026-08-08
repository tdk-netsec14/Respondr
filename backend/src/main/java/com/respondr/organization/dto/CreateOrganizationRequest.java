package com.respondr.organization.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new organization.
 * The slug must be URL-safe (lowercase letters, digits, hyphens).
 */
public record CreateOrganizationRequest(

        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @NotBlank(message = "slug must not be blank")
        @Pattern(
            regexp = "^[a-z0-9][a-z0-9-]{1,98}[a-z0-9]$",
            message = "slug must be 3-100 lowercase alphanumeric characters or hyphens, not starting/ending with a hyphen"
        )
        String slug
) {}
