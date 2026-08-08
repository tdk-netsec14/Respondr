package com.respondr.servicecatalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request DTO for registering a new service in the catalog. */
public record CreateServiceRequest(

        UUID teamId,

        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @NotBlank(message = "key must not be blank")
        @Pattern(
            regexp = "^[a-z0-9][a-z0-9-]{0,98}[a-z0-9]$|^[a-z0-9]$",
            message = "key must be lowercase alphanumeric characters or hyphens"
        )
        String key,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description
) {}
