package com.respondr.incident.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "body must not be blank")
        @Size(max = 2000, message = "body must be at most 2000 characters")
        String body
) {}
