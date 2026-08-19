package com.nhat.workflowhub.ai.tool;

import com.nhat.workflowhub.ai.dto.AllowedTool;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ToolRegistry {

  public List<AllowedTool> allowedTools() {
    return List.of(
        new AllowedTool("list_workflow_items", "List request items for the active organization or workspace."),
        new AllowedTool("create_workflow_item", "Create a new support request in the current workspace."),
        new AllowedTool("update_workflow_status", "Update the status of an existing request."),
        new AllowedTool("add_comment", "Add a comment to a request."),
        new AllowedTool("list_notifications", "List notifications for the current user."),
        new AllowedTool("mark_notification_read", "Mark a notification as read."),
        new AllowedTool("navigate_to", "Return a UI route suggestion instead of executing a write action.")
    );
  }
}
