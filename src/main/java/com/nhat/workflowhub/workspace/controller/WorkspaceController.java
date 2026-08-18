package com.nhat.workflowhub.workspace.controller;

import com.nhat.workflowhub.auth.security.AuthenticatedUser;
import com.nhat.workflowhub.workspace.dto.CreateWorkspaceRequest;
import com.nhat.workflowhub.workspace.dto.WorkspaceResponse;
import com.nhat.workflowhub.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/organizations/{orgSlug}/workspaces")
public class WorkspaceController {

  private final WorkspaceService workspaceService;

  public WorkspaceController(WorkspaceService workspaceService) {
    this.workspaceService = workspaceService;
  }

  @GetMapping
  public List<WorkspaceResponse> list(@PathVariable("orgSlug") String orgSlug, Authentication authentication) {
    return workspaceService.listWorkspaces(orgSlug, currentUserId(authentication));
  }

  @PostMapping
  public ResponseEntity<WorkspaceResponse> create(
      @PathVariable("orgSlug") String orgSlug,
      @Valid @RequestBody CreateWorkspaceRequest request,
      Authentication authentication
  ) {
    WorkspaceResponse response = workspaceService.createWorkspace(orgSlug, request, currentUserId(authentication));
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/{slug}")
        .buildAndExpand(response.slug())
        .toUri();
    return ResponseEntity.created(location).body(response);
  }

  private UUID currentUserId(Authentication authentication) {
    return ((AuthenticatedUser) authentication.getPrincipal()).userId();
  }
}
