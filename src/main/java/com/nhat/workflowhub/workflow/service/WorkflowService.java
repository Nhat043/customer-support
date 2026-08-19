package com.nhat.workflowhub.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhat.workflowhub.auth.entity.UserRole;
import com.nhat.workflowhub.auth.repository.UserAccountRepository;
import com.nhat.workflowhub.common.exception.ApiException;
import com.nhat.workflowhub.membership.entity.Membership;
import com.nhat.workflowhub.membership.repository.MembershipRepository;
import com.nhat.workflowhub.organization.entity.Organization;
import com.nhat.workflowhub.organization.service.OrganizationService;
import com.nhat.workflowhub.notification.service.NotificationService;
import com.nhat.workflowhub.workspace.entity.Workspace;
import com.nhat.workflowhub.workspace.repository.WorkspaceRepository;
import com.nhat.workflowhub.workflow.dto.CreateWorkflowItemRequest;
import com.nhat.workflowhub.workflow.dto.UpdateWorkflowItemRequest;
import com.nhat.workflowhub.workflow.dto.WorkflowEventResponse;
import com.nhat.workflowhub.workflow.dto.WorkflowItemResponse;
import com.nhat.workflowhub.workflow.entity.WorkflowEvent;
import com.nhat.workflowhub.workflow.entity.WorkflowItem;
import com.nhat.workflowhub.workflow.entity.WorkflowPriority;
import com.nhat.workflowhub.workflow.entity.WorkflowStatus;
import com.nhat.workflowhub.workflow.repository.WorkflowEventRepository;
import com.nhat.workflowhub.workflow.repository.WorkflowItemRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class WorkflowService {

  private final OrganizationService organizationService;
  private final WorkspaceRepository workspaceRepository;
  private final MembershipRepository membershipRepository;
  private final WorkflowItemRepository workflowItemRepository;
  private final WorkflowEventRepository workflowEventRepository;
  private final UserAccountRepository userAccountRepository;
  private final NotificationService notificationService;
  private final ObjectMapper objectMapper;

  public WorkflowService(
      OrganizationService organizationService,
      WorkspaceRepository workspaceRepository,
      MembershipRepository membershipRepository,
      WorkflowItemRepository workflowItemRepository,
      WorkflowEventRepository workflowEventRepository,
      UserAccountRepository userAccountRepository,
      NotificationService notificationService,
      ObjectMapper objectMapper
  ) {
    this.organizationService = organizationService;
    this.workspaceRepository = workspaceRepository;
    this.membershipRepository = membershipRepository;
    this.workflowItemRepository = workflowItemRepository;
    this.workflowEventRepository = workflowEventRepository;
    this.userAccountRepository = userAccountRepository;
    this.notificationService = notificationService;
    this.objectMapper = objectMapper;
  }

  @Transactional(readOnly = true)
  public List<WorkflowItemResponse> listWorkflowItems(String organizationSlug, String workspaceSlug, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    if (StringUtils.hasText(workspaceSlug)) {
      Workspace workspace = requireWorkspace(organization.getId(), workspaceSlug);
      return workflowItemRepository.findAllByOrganizationIdAndWorkspaceIdOrderByUpdatedAtDesc(
          organization.getId(),
          workspace.getId()
      ).stream().map(this::toResponse).toList();
    }

    return workflowItemRepository.findAllByOrganizationIdOrderByUpdatedAtDesc(organization.getId()).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public WorkflowItemResponse getWorkflowItem(String organizationSlug, UUID workflowItemId, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    WorkflowItem item = workflowItemRepository.findByIdAndOrganizationId(workflowItemId, organization.getId())
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workflow item not found"));
    return toResponse(item);
  }

  public WorkflowItemResponse createWorkflowItem(String organizationSlug, CreateWorkflowItemRequest request, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    requireCanWrite(organization, currentUserId);

    Workspace workspace = resolveWorkspaceForCreate(organization.getId(), request.workspaceSlug());
    WorkflowItem item = new WorkflowItem();
    item.setId(UUID.randomUUID());
    item.setOrganizationId(organization.getId());
    item.setWorkspaceId(workspace.getId());
    item.setCreatedByUserId(currentUserId);
    item.setTitle(request.title().trim());
    item.setDescription(request.description().trim());
    item.setStatus(request.status() == null ? WorkflowStatus.NEW : request.status());
    item.setPriority(request.priority() == null ? WorkflowPriority.MEDIUM : request.priority());
    item.setAssigneeUserId(request.assigneeUserId());
    validateAssignee(organization.getId(), item.getAssigneeUserId());
    workflowItemRepository.save(item);
    saveEvent(item, currentUserId, "CREATED", null, snapshot(item));
    notificationService.notifyWorkflowItemEvent(
        organization,
        item.getWorkspaceId(),
        item.getId(),
        currentUserId,
        "WORKFLOW_ITEM_CREATED",
        "New request: " + item.getTitle(),
        "A new workflow item was created in " + item.getWorkspaceId() + "."
    );
    return toResponse(item);
  }

  public WorkflowItemResponse updateWorkflowItem(
      String organizationSlug,
      UUID workflowItemId,
      UpdateWorkflowItemRequest request,
      UUID currentUserId
  ) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    requireCanWrite(organization, currentUserId);

    WorkflowItem item = workflowItemRepository.findByIdAndOrganizationId(workflowItemId, organization.getId())
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workflow item not found"));
    String before = snapshot(item);

    if (StringUtils.hasText(request.title())) {
      item.setTitle(request.title().trim());
    }
    if (StringUtils.hasText(request.description())) {
      item.setDescription(request.description().trim());
    }
    if (request.status() != null) {
      item.setStatus(request.status());
    }
    if (request.priority() != null) {
      item.setPriority(request.priority());
    }
    if (request.assigneeUserId() != null) {
      item.setAssigneeUserId(request.assigneeUserId());
      validateAssignee(organization.getId(), item.getAssigneeUserId());
    }
    if (request.dueAt() != null) {
      item.setDueAt(request.dueAt());
    }

    workflowItemRepository.save(item);
    saveEvent(item, currentUserId, "UPDATED", before, snapshot(item));
    notificationService.notifyWorkflowItemEvent(
        organization,
        item.getWorkspaceId(),
        item.getId(),
        currentUserId,
        "WORKFLOW_ITEM_UPDATED",
        "Request updated: " + item.getTitle(),
        "The request is now " + item.getStatus() + "."
    );
    return toResponse(item);
  }

  public void deleteWorkflowItem(String organizationSlug, UUID workflowItemId, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    requireCanWrite(organization, currentUserId);

    WorkflowItem item = workflowItemRepository.findByIdAndOrganizationId(workflowItemId, organization.getId())
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workflow item not found"));
    saveEvent(item, currentUserId, "DELETED", snapshot(item), null);
    notificationService.notifyWorkflowItemEvent(
        organization,
        item.getWorkspaceId(),
        item.getId(),
        currentUserId,
        "WORKFLOW_ITEM_DELETED",
        "Request deleted: " + item.getTitle(),
        "A workflow item was deleted from the workspace."
    );
    workflowItemRepository.delete(item);
  }

  @Transactional(readOnly = true)
  public List<WorkflowEventResponse> listEvents(String organizationSlug, UUID workflowItemId, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    WorkflowItem item = workflowItemRepository.findByIdAndOrganizationId(workflowItemId, organization.getId())
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workflow item not found"));
    return workflowEventRepository.findAllByWorkflowItemIdOrderByCreatedAtDesc(item.getId()).stream()
        .map(this::toEventResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public WorkflowItemResponse toResponse(WorkflowItem item) {
    return new WorkflowItemResponse(
        item.getId(),
        item.getOrganizationId(),
        item.getWorkspaceId(),
        item.getCreatedByUserId(),
        item.getTitle(),
        item.getDescription(),
        item.getStatus(),
        item.getPriority(),
        item.getAssigneeUserId(),
        item.getDueAt(),
        item.getCreatedAt(),
        item.getUpdatedAt()
    );
  }

  @Transactional(readOnly = true)
  public WorkflowEventResponse toEventResponse(WorkflowEvent event) {
    return new WorkflowEventResponse(
        event.getId(),
        event.getWorkflowItemId(),
        event.getEventType(),
        event.getOldValue(),
        event.getNewValue(),
        event.getActorUserId(),
        event.getCreatedAt()
    );
  }

  private Workspace requireWorkspace(UUID organizationId, String workspaceSlug) {
    return workspaceRepository.findByOrganizationIdAndSlug(organizationId, workspaceSlug)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workspace not found"));
  }

  private Workspace resolveWorkspaceForCreate(UUID organizationId, String workspaceSlug) {
    if (!StringUtils.hasText(workspaceSlug)) {
      return requireWorkspace(organizationId, "general");
    }
    return requireWorkspace(organizationId, workspaceSlug);
  }

  private void requireCanWrite(Organization organization, UUID currentUserId) {
    if (isOwner(organization, currentUserId)) {
      return;
    }

    List<Membership> memberships = membershipRepository.findAllByOrganizationIdAndUserId(organization.getId(), currentUserId);
    boolean canWrite = memberships.stream()
        .map(Membership::getRole)
        .anyMatch(role -> role == UserRole.OWNER || role == UserRole.ADMIN || role == UserRole.MEMBER);
    if (!canWrite) {
      throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to modify workflow items");
    }
  }

  private boolean isOwner(Organization organization, UUID userId) {
    return organization.getOwnerUserId().equals(userId);
  }

  private void validateAssignee(UUID organizationId, UUID assigneeUserId) {
    if (assigneeUserId == null) {
      return;
    }

    if (!userAccountRepository.existsById(assigneeUserId)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "Assignee user not found");
    }
    if (!membershipRepository.existsByOrganizationIdAndUserId(organizationId, assigneeUserId)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Assignee must belong to this organization");
    }
  }

  private void saveEvent(WorkflowItem item, UUID actorUserId, String eventType, String oldValue, String newValue) {
    WorkflowEvent event = new WorkflowEvent();
    event.setId(UUID.randomUUID());
    event.setWorkflowItemId(item.getId());
    event.setOrganizationId(item.getOrganizationId());
    event.setWorkspaceId(item.getWorkspaceId());
    event.setEventType(eventType);
    event.setOldValue(oldValue);
    event.setNewValue(newValue);
    event.setActorUserId(actorUserId);
    workflowEventRepository.save(event);
  }

  private String snapshot(WorkflowItem item) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("id", item.getId());
    data.put("workspaceId", item.getWorkspaceId());
    data.put("title", item.getTitle());
    data.put("description", item.getDescription());
    data.put("status", item.getStatus());
    data.put("priority", item.getPriority());
    data.put("assigneeUserId", item.getAssigneeUserId());
    data.put("dueAt", item.getDueAt());
    try {
      return objectMapper.writeValueAsString(data);
    } catch (Exception exception) {
      return data.toString();
    }
  }
}
