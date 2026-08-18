package com.nhat.workflowhub.attachment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AttachmentResponse(
    UUID id,
    UUID workflowItemId,
    UUID organizationId,
    UUID workspaceId,
    UUID uploadedByUserId,
    String fileName,
    String contentType,
    Long fileSize,
    String storageProvider,
    String storageKey,
    String checksum,
    OffsetDateTime createdAt,
    OffsetDateTime deletedAt
) {
}
