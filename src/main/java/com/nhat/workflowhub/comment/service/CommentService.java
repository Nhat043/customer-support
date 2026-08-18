package com.nhat.workflowhub.comment.service;

import com.nhat.workflowhub.auth.entity.UserRole;
import com.nhat.workflowhub.common.exception.ApiException;
import com.nhat.workflowhub.comment.dto.CommentResponse;
import com.nhat.workflowhub.comment.dto.CreateCommentRequest;
import com.nhat.workflowhub.comment.dto.UpdateCommentRequest;
import com.nhat.workflowhub.comment.entity.Comment;
import com.nhat.workflowhub.comment.repository.CommentRepository;
import com.nhat.workflowhub.membership.entity.Membership;
import com.nhat.workflowhub.membership.repository.MembershipRepository;
import com.nhat.workflowhub.organization.entity.Organization;
import com.nhat.workflowhub.organization.service.OrganizationService;
import com.nhat.workflowhub.workflow.entity.WorkflowItem;
import com.nhat.workflowhub.workflow.repository.WorkflowItemRepository;
import com.nhat.workflowhub.workspace.entity.Workspace;
import com.nhat.workflowhub.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CommentService {

  private final OrganizationService organizationService;
  private final WorkspaceRepository workspaceRepository;
  private final MembershipRepository membershipRepository;
  private final WorkflowItemRepository workflowItemRepository;
  private final CommentRepository commentRepository;

  public CommentService(
      OrganizationService organizationService,
      WorkspaceRepository workspaceRepository,
      MembershipRepository membershipRepository,
      WorkflowItemRepository workflowItemRepository,
      CommentRepository commentRepository
  ) {
    this.organizationService = organizationService;
    this.workspaceRepository = workspaceRepository;
    this.membershipRepository = membershipRepository;
    this.workflowItemRepository = workflowItemRepository;
    this.commentRepository = commentRepository;
  }

  @Transactional(readOnly = true)
  public List<CommentResponse> listComments(String organizationSlug, UUID workflowItemId, UUID currentUserId) {
    WorkflowContext context = requireContext(organizationSlug, workflowItemId, currentUserId);
    return commentRepository.findAllByWorkflowItemIdOrderByCreatedAtAsc(context.workflowItem().getId()).stream()
        .map(this::toResponse)
        .toList();
  }

  public CommentResponse addComment(String organizationSlug, UUID workflowItemId, CreateCommentRequest request, UUID currentUserId) {
    WorkflowContext context = requireContext(organizationSlug, workflowItemId, currentUserId);
    requireCanWrite(context.organization(), currentUserId);

    Comment comment = new Comment();
    comment.setId(UUID.randomUUID());
    comment.setWorkflowItemId(context.workflowItem().getId());
    comment.setOrganizationId(context.organization().getId());
    comment.setWorkspaceId(context.workflowItem().getWorkspaceId());
    comment.setUserId(currentUserId);
    comment.setBody(request.body().trim());
    commentRepository.save(comment);
    return toResponse(comment);
  }

  public CommentResponse updateComment(String organizationSlug, UUID workflowItemId, UUID commentId, UpdateCommentRequest request, UUID currentUserId) {
    WorkflowContext context = requireContext(organizationSlug, workflowItemId, currentUserId);
    requireCanWrite(context.organization(), currentUserId);

    Comment comment = commentRepository.findByIdAndWorkflowItemId(commentId, workflowItemId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found"));
    ensureSameOrganization(comment, context.organization().getId());
    comment.setBody(request.body().trim());
    commentRepository.save(comment);
    return toResponse(comment);
  }

  public void deleteComment(String organizationSlug, UUID workflowItemId, UUID commentId, UUID currentUserId) {
    WorkflowContext context = requireContext(organizationSlug, workflowItemId, currentUserId);
    requireCanWrite(context.organization(), currentUserId);

    Comment comment = commentRepository.findByIdAndWorkflowItemId(commentId, workflowItemId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Comment not found"));
    ensureSameOrganization(comment, context.organization().getId());
    commentRepository.delete(comment);
  }

  @Transactional(readOnly = true)
  public CommentResponse toResponse(Comment comment) {
    return new CommentResponse(
        comment.getId(),
        comment.getWorkflowItemId(),
        comment.getOrganizationId(),
        comment.getWorkspaceId(),
        comment.getUserId(),
        comment.getBody(),
        comment.getCreatedAt(),
        comment.getUpdatedAt()
    );
  }

  private WorkflowContext requireContext(String organizationSlug, UUID workflowItemId, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    WorkflowItem workflowItem = workflowItemRepository.findByIdAndOrganizationId(workflowItemId, organization.getId())
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workflow item not found"));
    return new WorkflowContext(organization, workflowItem);
  }

  private void requireCanWrite(Organization organization, UUID currentUserId) {
    if (organization.getOwnerUserId().equals(currentUserId)) {
      return;
    }

    List<Membership> memberships = membershipRepository.findAllByOrganizationIdAndUserId(organization.getId(), currentUserId);
    boolean canWrite = memberships.stream()
        .map(Membership::getRole)
        .anyMatch(role -> role == UserRole.OWNER || role == UserRole.ADMIN || role == UserRole.MEMBER);
    if (!canWrite) {
      throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to modify comments");
    }
  }

  private void ensureSameOrganization(Comment comment, UUID organizationId) {
    if (!organizationId.equals(comment.getOrganizationId())) {
      throw new ApiException(HttpStatus.NOT_FOUND, "Comment not found");
    }
  }

  private record WorkflowContext(Organization organization, WorkflowItem workflowItem) {
  }
}
