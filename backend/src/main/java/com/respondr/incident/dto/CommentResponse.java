package com.respondr.incident.dto;

import com.respondr.incident.Comment;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID incidentId,
        UUID authorId,
        String authorName,
        String body,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getIncident().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getName(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
