package com.nhat.workflowhub.attachment.controller;

import com.nhat.workflowhub.auth.security.AuthenticatedUser;
import com.nhat.workflowhub.attachment.dto.AttachmentResponse;
import com.nhat.workflowhub.attachment.service.AttachmentService;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/organizations/{orgSlug}/workflow-items/{workflowItemId}/attachments")
public class AttachmentController {

  private final AttachmentService attachmentService;

  public AttachmentController(AttachmentService attachmentService) {
    this.attachmentService = attachmentService;
  }

  @GetMapping
  public List<AttachmentResponse> list(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("workflowItemId") UUID workflowItemId,
      Authentication authentication
  ) {
    return attachmentService.listAttachments(orgSlug, workflowItemId, currentUserId(authentication));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public AttachmentResponse upload(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("workflowItemId") UUID workflowItemId,
      @RequestPart("file") MultipartFile file,
      Authentication authentication
  ) {
    return attachmentService.uploadAttachment(orgSlug, workflowItemId, file, currentUserId(authentication));
  }

  @GetMapping("/{attachmentId}/download")
  public ResponseEntity<Resource> download(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("workflowItemId") UUID workflowItemId,
      @PathVariable("attachmentId") UUID attachmentId,
      Authentication authentication
  ) {
    AttachmentResponse attachment = attachmentService.getAttachment(orgSlug, workflowItemId, attachmentId, currentUserId(authentication));
    Resource resource = attachmentService.downloadAttachment(orgSlug, workflowItemId, attachmentId, currentUserId(authentication));
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(attachment.contentType()))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(attachment.fileName(), StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(resource);
  }

  @DeleteMapping("/{attachmentId}")
  public ResponseEntity<Void> delete(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("workflowItemId") UUID workflowItemId,
      @PathVariable("attachmentId") UUID attachmentId,
      Authentication authentication
  ) {
    attachmentService.deleteAttachment(orgSlug, workflowItemId, attachmentId, currentUserId(authentication));
    return ResponseEntity.noContent().build();
  }

  private UUID currentUserId(Authentication authentication) {
    return ((AuthenticatedUser) authentication.getPrincipal()).userId();
  }
}
