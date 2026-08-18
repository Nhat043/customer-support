package com.nhat.workflowhub.workspace.service;

import com.nhat.workflowhub.common.exception.ApiException;
import com.nhat.workflowhub.organization.entity.Organization;
import com.nhat.workflowhub.organization.service.OrganizationService;
import com.nhat.workflowhub.workspace.dto.CreateWorkspaceRequest;
import com.nhat.workflowhub.workspace.dto.WorkspaceResponse;
import com.nhat.workflowhub.workspace.entity.Workspace;
import com.nhat.workflowhub.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class WorkspaceService {

  private final OrganizationService organizationService;
  private final WorkspaceRepository workspaceRepository;

  public WorkspaceService(OrganizationService organizationService, WorkspaceRepository workspaceRepository) {
    this.organizationService = organizationService;
    this.workspaceRepository = workspaceRepository;
  }

  @Transactional(readOnly = true)
  public List<WorkspaceResponse> listWorkspaces(String organizationSlug, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    return workspaceRepository.findAllByOrganizationId(organization.getId()).stream()
        .map(organizationService::toWorkspaceResponse)
        .toList();
  }

  public WorkspaceResponse createWorkspace(String organizationSlug, CreateWorkspaceRequest request, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    if (!organizationService.canManageOrganization(organization, currentUserId)) {
      throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to create workspaces");
    }

    String slug = normalizeSlug(request.slug(), request.name());
    if (workspaceRepository.existsByOrganizationIdAndSlug(organization.getId(), slug)) {
      throw new ApiException(HttpStatus.CONFLICT, "Workspace slug already exists in this organization");
    }

    Workspace workspace = new Workspace();
    workspace.setId(UUID.randomUUID());
    workspace.setOrganizationId(organization.getId());
    workspace.setName(request.name().trim());
    workspace.setSlug(slug);
    workspaceRepository.save(workspace);
    return organizationService.toWorkspaceResponse(workspace);
  }

  private String normalizeSlug(String explicitSlug, String fallbackName) {
    String source = StringUtils.hasText(explicitSlug) ? explicitSlug : fallbackName;
    String slug = source.trim().toLowerCase()
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("^-+|-+$", "");
    if (!StringUtils.hasText(slug)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Slug cannot be empty");
    }
    return slug;
  }
}
