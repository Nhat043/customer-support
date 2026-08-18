package com.nhat.workflowhub.workflow.dto;

import com.nhat.workflowhub.workflow.entity.WorkflowPriority;
import com.nhat.workflowhub.workflow.entity.WorkflowStatus;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UpdateWorkflowItemRequest(
    String title,
    String description,
    WorkflowStatus status,
    WorkflowPriority priority,
    UUID assigneeUserId,
    OffsetDateTime dueAt
) {
}
