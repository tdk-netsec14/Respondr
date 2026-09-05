package com.respondr.escalation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreatePolicyRequest(
        @NotNull(message = "teamId must not be null")
        UUID teamId,

        @NotBlank(message = "name must not be blank")
        String name
) {}
