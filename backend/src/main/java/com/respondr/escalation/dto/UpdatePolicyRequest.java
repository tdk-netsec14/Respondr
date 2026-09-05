package com.respondr.escalation.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePolicyRequest(
        @NotBlank(message = "name must not be blank")
        String name,

        boolean active
) {}
