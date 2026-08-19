package com.nhat.workflowhub.attachment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nhat.workflowhub.attachment.entity.Attachment;
import com.nhat.workflowhub.attachment.repository.AttachmentRepository;
import com.nhat.workflowhub.auth.entity.UserRole;
import com.nhat.workflowhub.membership.entity.Membership;
import com.nhat.workflowhub.membership.repository.MembershipRepository;
import com.nhat.workflowhub.notification.service.NotificationService;
import com.nhat.workflowhub.organization.entity.Organization;
import com.nhat.workflowhub.organization.service.OrganizationService;
import com.nhat.workflowhub.workflow.entity.WorkflowItem;
import com.nhat.workflowhub.workflow.repository.WorkflowItemRepository;
import com.nhat.workflowhub.workspace.repository.WorkspaceRepository;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

  @Mock
  private OrganizationService organizationService;
  @Mock
  private WorkspaceRepository workspaceRepository;
  @Mock
  private MembershipRepository membershipRepository;
  @Mock
  private WorkflowItemRepository workflowItemRepository;
  @Mock
  private AttachmentRepository attachmentRepository;
  @Mock
  private NotificationService notificationService;

  @TempDir
  Path tempDir;

  @Test
  void deleteAttachment_softDeletesMetadataAndRemovesFileFromDisk() throws Exception {
    UUID currentUserId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();
    UUID workspaceId = UUID.randomUUID();
    UUID attachmentId = UUID.randomUUID();

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
    workflowItem.setStatus(com.nhat.workflowhub.workflow.entity.WorkflowStatus.NEW);
    workflowItem.setPriority(com.nhat.workflowhub.workflow.entity.WorkflowPriority.MEDIUM);

    Attachment attachment = new Attachment();
    attachment.setId(attachmentId);
    attachment.setWorkflowItemId(workflowItemId);
    attachment.setOrganizationId(organizationId);
    attachment.setWorkspaceId(workspaceId);
    attachment.setUploadedByUserId(currentUserId);
    attachment.setFileName("playbook.md");
    attachment.setContentType("text/markdown");
    attachment.setFileSize(12L);
    attachment.setStorageProvider("LOCAL");
    attachment.setStorageKey("acme/" + workflowItemId + "/playbook.md");
    attachment.setChecksum("checksum");
    attachment.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

    Path storedFile = tempDir.resolve(attachment.getStorageKey());
    Files.createDirectories(storedFile.getParent());
    Files.writeString(storedFile, "hello attachment", StandardCharsets.UTF_8);

    when(organizationService.requireAccessibleOrganization("acme", currentUserId)).thenReturn(organization);
    when(workflowItemRepository.findByIdAndOrganizationId(workflowItemId, organizationId)).thenReturn(Optional.of(workflowItem));
    when(attachmentRepository.findByIdAndWorkflowItemIdAndDeletedAtIsNull(attachmentId, workflowItemId)).thenReturn(Optional.of(attachment));
    when(attachmentRepository.save(any(Attachment.class))).thenAnswer(invocation -> invocation.getArgument(0));

    AttachmentService service = new AttachmentService(
        organizationService,
        workspaceRepository,
        membershipRepository,
        workflowItemRepository,
        attachmentRepository,
        notificationService,
        tempDir.toString()
    );

    service.deleteAttachment("acme", workflowItemId, attachmentId, currentUserId);

    assertThat(attachment.getDeletedAt()).isNotNull();
    assertThat(Files.exists(storedFile)).isFalse();
    verify(attachmentRepository).save(attachment);
  }
}
