package com.nhat.workflowhub.workflow.dto;

import com.nhat.workflowhub.workflow.entity.WorkflowPriority;
import com.nhat.workflowhub.workflow.entity.WorkflowStatus;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CreateWorkflowItemRequest(
    @NotBlank String title,
    @NotBlank String description,
    WorkflowStatus status,
    WorkflowPriority priority,
    String workspaceSlug,
    UUID assigneeUserId
) {
  public CreateWorkflowItemRequest {
    if (status == null) {
      status = WorkflowStatus.NEW;
    }
    if (priority == null) {
      priority = WorkflowPriority.MEDIUM;
    }
  }
}
