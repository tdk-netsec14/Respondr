package com.respondr.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request DTO for creating a team within an organization. */
public record CreateTeamRequest(

        @NotBlank(message = "name must not be blank")
        @Size(max = 255, message = "name must be at most 255 characters")
        String name,

        @Size(max = 2000, message = "description must be at most 2000 characters")
        String description
) {}
