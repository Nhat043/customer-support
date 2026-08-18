package com.nhat.workflowhub.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhat.workflowhub.auth.repository.UserAccountRepository;
import com.nhat.workflowhub.membership.repository.MembershipRepository;
import com.nhat.workflowhub.organization.entity.Organization;
import com.nhat.workflowhub.organization.service.OrganizationService;
import com.nhat.workflowhub.workflow.dto.UpdateWorkflowItemRequest;
import com.nhat.workflowhub.workflow.entity.WorkflowEvent;
import com.nhat.workflowhub.workflow.entity.WorkflowItem;
import com.nhat.workflowhub.workflow.entity.WorkflowPriority;
import com.nhat.workflowhub.workflow.entity.WorkflowStatus;
import com.nhat.workflowhub.workflow.repository.WorkflowEventRepository;
import com.nhat.workflowhub.workflow.repository.WorkflowItemRepository;
import com.nhat.workflowhub.workspace.repository.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceTest {

  @Mock
  private OrganizationService organizationService;
  @Mock
  private WorkspaceRepository workspaceRepository;
  @Mock
  private MembershipRepository membershipRepository;
  @Mock
  private WorkflowItemRepository workflowItemRepository;
  @Mock
  private WorkflowEventRepository workflowEventRepository;
  @Mock
  private UserAccountRepository userAccountRepository;
  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private WorkflowService workflowService;

  @Test
  void updateWorkflowItem_keepsExistingAssigneeWhenRequestDoesNotSetOne() {
    UUID currentUserId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();
    UUID assigneeUserId = UUID.randomUUID();

    Organization organization = new Organization();
    organization.setId(organizationId);
    organization.setName("Acme");
    organization.setSlug("acme");
    organization.setOwnerUserId(currentUserId);

    WorkflowItem item = new WorkflowItem();
    item.setId(workflowItemId);
    item.setOrganizationId(organizationId);
    item.setWorkspaceId(UUID.randomUUID());
    item.setCreatedByUserId(currentUserId);
    item.setTitle("Original");
    item.setDescription("Original description");
    item.setStatus(WorkflowStatus.NEW);
    item.setPriority(WorkflowPriority.MEDIUM);
    item.setAssigneeUserId(assigneeUserId);
    item.setDueAt(OffsetDateTime.now(ZoneOffset.UTC));

    when(organizationService.requireAccessibleOrganization("acme", currentUserId)).thenReturn(organization);
    when(workflowItemRepository.findByIdAndOrganizationId(workflowItemId, organizationId)).thenReturn(Optional.of(item));
    when(workflowItemRepository.save(any(WorkflowItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

    workflowService.updateWorkflowItem(
        "acme",
        workflowItemId,
        new UpdateWorkflowItemRequest(null, null, null, null, null, null),
        currentUserId
    );

    assertThat(item.getAssigneeUserId()).isEqualTo(assigneeUserId);
    verify(workflowItemRepository).save(item);
    verify(workflowEventRepository).save(any(WorkflowEvent.class));
  }
}
