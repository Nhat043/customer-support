package com.nhat.workflowhub.workflow.controller;

import com.nhat.workflowhub.auth.security.AuthenticatedUser;
import com.nhat.workflowhub.workflow.dto.CreateWorkflowItemRequest;
import com.nhat.workflowhub.workflow.dto.UpdateWorkflowItemRequest;
import com.nhat.workflowhub.workflow.dto.WorkflowEventResponse;
import com.nhat.workflowhub.workflow.dto.WorkflowItemResponse;
import com.nhat.workflowhub.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/organizations/{orgSlug}/workflow-items")
public class WorkflowController {

  private final WorkflowService workflowService;

  public WorkflowController(WorkflowService workflowService) {
    this.workflowService = workflowService;
  }

  @GetMapping
  public List<WorkflowItemResponse> list(
      @PathVariable("orgSlug") String orgSlug,
      @RequestParam(required = false) String workspaceSlug,
      Authentication authentication
  ) {
    return workflowService.listWorkflowItems(orgSlug, workspaceSlug, currentUserId(authentication));
  }

  @GetMapping("/{workflowItemId}")
  public WorkflowItemResponse get(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("workflowItemId") UUID workflowItemId,
      Authentication authentication
  ) {
    return workflowService.getWorkflowItem(orgSlug, workflowItemId, currentUserId(authentication));
  }

  @PostMapping
  public ResponseEntity<WorkflowItemResponse> create(
      @PathVariable("orgSlug") String orgSlug,
      @Valid @RequestBody CreateWorkflowItemRequest request,
      Authentication authentication
  ) {
    WorkflowItemResponse response = workflowService.createWorkflowItem(orgSlug, request, currentUserId(authentication));
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{workflowItemId}")
        .buildAndExpand(response.id())
        .toUri();
    return ResponseEntity.created(location).body(response);
  }

  @PatchMapping("/{workflowItemId}")
  public WorkflowItemResponse update(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("workflowItemId") UUID workflowItemId,
      @Valid @RequestBody UpdateWorkflowItemRequest request,
      Authentication authentication
  ) {
    return workflowService.updateWorkflowItem(orgSlug, workflowItemId, request, currentUserId(authentication));
  }

  @DeleteMapping("/{workflowItemId}")
  public ResponseEntity<Void> delete(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("workflowItemId") UUID workflowItemId,
      Authentication authentication
  ) {
    workflowService.deleteWorkflowItem(orgSlug, workflowItemId, currentUserId(authentication));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/{workflowItemId}/events")
  public List<WorkflowEventResponse> listEvents(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("workflowItemId") UUID workflowItemId,
      Authentication authentication
  ) {
    return workflowService.listEvents(orgSlug, workflowItemId, currentUserId(authentication));
  }

  private UUID currentUserId(Authentication authentication) {
    return ((AuthenticatedUser) authentication.getPrincipal()).userId();
  }
}
