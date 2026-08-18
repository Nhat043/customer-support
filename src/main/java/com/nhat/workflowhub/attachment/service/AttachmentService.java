package com.nhat.workflowhub.attachment.service;

import com.nhat.workflowhub.auth.entity.UserRole;
import com.nhat.workflowhub.attachment.dto.AttachmentResponse;
import com.nhat.workflowhub.attachment.entity.Attachment;
import com.nhat.workflowhub.attachment.repository.AttachmentRepository;
import com.nhat.workflowhub.common.exception.ApiException;
import com.nhat.workflowhub.membership.entity.Membership;
import com.nhat.workflowhub.membership.repository.MembershipRepository;
import com.nhat.workflowhub.organization.entity.Organization;
import com.nhat.workflowhub.organization.service.OrganizationService;
import com.nhat.workflowhub.workflow.entity.WorkflowItem;
import com.nhat.workflowhub.workflow.repository.WorkflowItemRepository;
import com.nhat.workflowhub.workspace.entity.Workspace;
import com.nhat.workflowhub.workspace.repository.WorkspaceRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class AttachmentService {

  private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

  private final OrganizationService organizationService;
  private final WorkspaceRepository workspaceRepository;
  private final MembershipRepository membershipRepository;
  private final WorkflowItemRepository workflowItemRepository;
  private final AttachmentRepository attachmentRepository;
  private final Path storageRoot;

  public AttachmentService(
      OrganizationService organizationService,
      WorkspaceRepository workspaceRepository,
      MembershipRepository membershipRepository,
      WorkflowItemRepository workflowItemRepository,
      AttachmentRepository attachmentRepository,
      @Value("${app.attachments.dir:./data/attachments}") String storageDir
  ) {
    this.organizationService = organizationService;
    this.workspaceRepository = workspaceRepository;
    this.membershipRepository = membershipRepository;
    this.workflowItemRepository = workflowItemRepository;
    this.attachmentRepository = attachmentRepository;
    this.storageRoot = Path.of(storageDir);
  }

  @Transactional(readOnly = true)
  public List<AttachmentResponse> listAttachments(String organizationSlug, UUID workflowItemId, UUID currentUserId) {
    WorkflowContext context = requireContext(organizationSlug, workflowItemId, currentUserId);
    return attachmentRepository.findAllByWorkflowItemIdAndDeletedAtIsNullOrderByCreatedAtDesc(context.workflowItem().getId()).stream()
        .map(this::toResponse)
        .toList();
  }

  public AttachmentResponse uploadAttachment(String organizationSlug, UUID workflowItemId, MultipartFile file, UUID currentUserId) {
    WorkflowContext context = requireContext(organizationSlug, workflowItemId, currentUserId);
    requireCanWrite(context.organization(), currentUserId);
    requireFile(file);

    String extension = extractExtension(file.getOriginalFilename());
    String storageKey = context.organization().getSlug() + "/"
        + context.workflowItem().getId() + "/"
        + UUID.randomUUID() + extension;
    Path destination = storageRoot.resolve(storageKey);

    try {
      Files.createDirectories(destination.getParent());
      file.transferTo(destination);
    } catch (IOException exception) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store attachment");
    }

    Attachment attachment = new Attachment();
    attachment.setId(UUID.randomUUID());
    attachment.setWorkflowItemId(context.workflowItem().getId());
    attachment.setOrganizationId(context.organization().getId());
    attachment.setWorkspaceId(context.workflowItem().getWorkspaceId());
    attachment.setUploadedByUserId(currentUserId);
    attachment.setFileName(StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "attachment");
    attachment.setContentType(StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream");
    attachment.setFileSize(file.getSize());
    attachment.setStorageProvider("LOCAL");
    attachment.setStorageKey(storageKey.replace('\\', '/'));
    attachment.setChecksum(checksum(file));
    attachmentRepository.save(attachment);
    return toResponse(attachment);
  }

  public Resource downloadAttachment(String organizationSlug, UUID workflowItemId, UUID attachmentId, UUID currentUserId) {
    WorkflowContext context = requireContext(organizationSlug, workflowItemId, currentUserId);
    Attachment attachment = requireActiveAttachment(context.workflowItem().getId(), attachmentId);
    Path path = storageRoot.resolve(attachment.getStorageKey());
    if (!Files.exists(path)) {
      throw new ApiException(HttpStatus.NOT_FOUND, "Attachment file not found");
    }
    return new FileSystemResource(path);
  }

  @Transactional(readOnly = true)
  public AttachmentResponse getAttachment(String organizationSlug, UUID workflowItemId, UUID attachmentId, UUID currentUserId) {
    WorkflowContext context = requireContext(organizationSlug, workflowItemId, currentUserId);
    Attachment attachment = requireActiveAttachment(context.workflowItem().getId(), attachmentId);
    return toResponse(attachment);
  }

  public void deleteAttachment(String organizationSlug, UUID workflowItemId, UUID attachmentId, UUID currentUserId) {
    WorkflowContext context = requireContext(organizationSlug, workflowItemId, currentUserId);
    requireCanWrite(context.organization(), currentUserId);
    Attachment attachment = requireActiveAttachment(context.workflowItem().getId(), attachmentId);
    attachment.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
    attachmentRepository.save(attachment);

    Path path = storageRoot.resolve(attachment.getStorageKey());
    try {
      Files.deleteIfExists(path);
    } catch (IOException exception) {
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete attachment file");
    }
  }

  @Transactional(readOnly = true)
  public AttachmentResponse toResponse(Attachment attachment) {
    return new AttachmentResponse(
        attachment.getId(),
        attachment.getWorkflowItemId(),
        attachment.getOrganizationId(),
        attachment.getWorkspaceId(),
        attachment.getUploadedByUserId(),
        attachment.getFileName(),
        attachment.getContentType(),
        attachment.getFileSize(),
        attachment.getStorageProvider(),
        attachment.getStorageKey(),
        attachment.getChecksum(),
        attachment.getCreatedAt(),
        attachment.getDeletedAt()
    );
  }

  private WorkflowContext requireContext(String organizationSlug, UUID workflowItemId, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    WorkflowItem workflowItem = workflowItemRepository.findByIdAndOrganizationId(workflowItemId, organization.getId())
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Workflow item not found"));
    return new WorkflowContext(organization, workflowItem);
  }

  private Attachment requireAttachment(UUID workflowItemId, UUID attachmentId) {
    return attachmentRepository.findByIdAndWorkflowItemId(attachmentId, workflowItemId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attachment not found"));
  }

  private Attachment requireActiveAttachment(UUID workflowItemId, UUID attachmentId) {
    return attachmentRepository.findByIdAndWorkflowItemIdAndDeletedAtIsNull(attachmentId, workflowItemId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Attachment not found"));
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
      throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to modify attachments");
    }
  }

  private void requireFile(MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Attachment file is required");
    }
    if (file.getSize() > MAX_FILE_SIZE_BYTES) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "Attachment must be 10 MB or smaller");
    }
  }

  private String extractExtension(String originalFilename) {
    if (!StringUtils.hasText(originalFilename)) {
      return "";
    }
    int dotIndex = originalFilename.lastIndexOf('.');
    return dotIndex >= 0 ? originalFilename.substring(dotIndex) : "";
  }

  private String checksum(MultipartFile file) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (InputStream inputStream = file.getInputStream()) {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
          digest.update(buffer, 0, read);
        }
      }
      return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest());
    } catch (IOException | NoSuchAlgorithmException exception) {
      return null;
    }
  }

  private record WorkflowContext(Organization organization, WorkflowItem workflowItem) {
  }
}
