package com.nhat.workflowhub.notification.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID organizationId,
    UUID workspaceId,
    UUID userId,
    String type,
    String title,
    String body,
    String entityType,
    UUID entityId,
    OffsetDateTime readAt,
    OffsetDateTime createdAt
) {
}
