package com.respondr.servicecatalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/** Request DTO for updating a service's mutable fields. */
public record UpdateServiceRequest(

        UUID teamId,

        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description,

        boolean active
) {}
