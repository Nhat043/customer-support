package com.nhat.workflowhub.workflow.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkflowEventResponse(
    UUID id,
    UUID workflowItemId,
    String eventType,
    String oldValue,
    String newValue,
    UUID actorUserId,
    OffsetDateTime createdAt
) {
}
