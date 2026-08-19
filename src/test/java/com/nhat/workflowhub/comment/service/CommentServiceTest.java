package com.nhat.workflowhub.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nhat.workflowhub.auth.entity.UserRole;
import com.nhat.workflowhub.comment.dto.CreateCommentRequest;
import com.nhat.workflowhub.comment.entity.Comment;
import com.nhat.workflowhub.comment.repository.CommentRepository;
import com.nhat.workflowhub.membership.entity.Membership;
import com.nhat.workflowhub.membership.repository.MembershipRepository;
import com.nhat.workflowhub.notification.service.NotificationService;
import com.nhat.workflowhub.organization.entity.Organization;
import com.nhat.workflowhub.organization.service.OrganizationService;
import com.nhat.workflowhub.workflow.entity.WorkflowItem;
import com.nhat.workflowhub.workflow.entity.WorkflowPriority;
import com.nhat.workflowhub.workflow.entity.WorkflowStatus;
import com.nhat.workflowhub.workflow.repository.WorkflowItemRepository;
import com.nhat.workflowhub.workspace.repository.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

  @Mock
  private OrganizationService organizationService;
  @Mock
  private WorkspaceRepository workspaceRepository;
  @Mock
  private MembershipRepository membershipRepository;
  @Mock
  private WorkflowItemRepository workflowItemRepository;
  @Mock
  private CommentRepository commentRepository;
  @Mock
  private NotificationService notificationService;

  @Test
  void addComment_trimsBodyAndSavesInSameWorkflowItem() {
    UUID currentUserId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();
    UUID workspaceId = UUID.randomUUID();

    Organization organization = new Organization();
    organization.setId(organizationId);
    organization.setName("Acme");
    organization.setSlug("acme");
    organization.setOwnerUserId(currentUserId);

    WorkflowItem workflowItem = new WorkflowItem();
    workflowItem.setId(workflowItemId);
    workflowItem.setOrganizationId(organizationId);
    workflowItem.setWorkspaceId(workspaceId);
    workflowItem.setCreatedByUserId(currentUserId);
    workflowItem.setTitle("First");
    workflowItem.setDescription("First issue");
    workflowItem.setStatus(WorkflowStatus.NEW);
    workflowItem.setPriority(WorkflowPriority.MEDIUM);

    when(organizationService.requireAccessibleOrganization("acme", currentUserId)).thenReturn(organization);
    when(workflowItemRepository.findByIdAndOrganizationId(workflowItemId, organizationId)).thenReturn(Optional.of(workflowItem));
    when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

    Membership ownerMembership = new Membership();
    ownerMembership.setId(UUID.randomUUID());
    ownerMembership.setOrganizationId(organizationId);
    ownerMembership.setWorkspaceId(null);
    ownerMembership.setUserId(currentUserId);
    ownerMembership.setRole(UserRole.OWNER);
    ownerMembership.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    ownerMembership.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

    when(membershipRepository.findAllByOrganizationIdAndUserId(organizationId, currentUserId)).thenReturn(List.of(ownerMembership));

    CommentService commentService = new CommentService(
        organizationService,
        workspaceRepository,
        membershipRepository,
        workflowItemRepository,
        commentRepository,
        notificationService
    );

    var response = commentService.addComment(
        "acme",
        workflowItemId,
        new CreateCommentRequest("  hello from member  "),
        currentUserId
    );

    assertThat(response.body()).isEqualTo("hello from member");
    assertThat(response.workflowItemId()).isEqualTo(workflowItemId);
    verify(commentRepository).save(any(Comment.class));
  }
}
