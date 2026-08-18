package com.nhat.workflowhub.comment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record CommentResponse(
    UUID id,
    UUID workflowItemId,
    UUID organizationId,
    UUID workspaceId,
    UUID userId,
    String body,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
