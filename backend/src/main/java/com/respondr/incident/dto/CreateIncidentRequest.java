package com.respondr.incident.dto;

import com.respondr.incident.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateIncidentRequest(
        @NotBlank(message = "title must not be blank")
        @Size(max = 500, message = "title must be at most 500 characters")
        String title,

        @Size(max = 4000, message = "description must be at most 4000 characters")
        String description,

        @NotNull(message = "severity must not be null")
        IncidentSeverity severity,

        UUID serviceId,

        UUID teamId
) {}
