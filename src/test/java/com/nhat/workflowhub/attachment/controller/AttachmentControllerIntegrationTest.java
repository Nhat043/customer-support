package com.nhat.workflowhub.attachment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nhat.workflowhub.auth.entity.UserAccount;
import com.nhat.workflowhub.auth.entity.UserStatus;
import com.nhat.workflowhub.auth.repository.UserAccountRepository;
import com.nhat.workflowhub.auth.security.JwtService;
import com.nhat.workflowhub.attachment.dto.AttachmentResponse;
import com.nhat.workflowhub.attachment.service.AttachmentService;
import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.test.web.servlet.MockMvc;
import org.hamcrest.Matchers;

@SpringBootTest
@AutoConfigureMockMvc
class AttachmentControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtService jwtService;
  @MockBean
  private UserAccountRepository userAccountRepository;
  @MockBean
  private AttachmentService attachmentService;

  @Test
  void uploadAttachment_returnsCreatedAttachmentMetadata() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID attachmentId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID workspaceId = UUID.randomUUID();

    UserAccount user = new UserAccount();
    user.setId(userId);
    user.setEmail("nhat@example.com");
    user.setFullName("Nhat");
    user.setPasswordHash("hashed");
    user.setStatus(UserStatus.ACTIVE);
    user.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));

    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(attachmentService.uploadAttachment(any(), any(), any(), any())).thenReturn(
        new AttachmentResponse(
            attachmentId,
            workflowItemId,
            organizationId,
            workspaceId,
            userId,
            "playbook.md",
            "text/markdown",
            123L,
            "LOCAL",
            "acme/workflow/playbook.md",
            "checksum",
            OffsetDateTime.now(ZoneOffset.UTC),
            null
        )
    );

    MockMultipartFile file = new MockMultipartFile(
        "file",
        "playbook.md",
        MediaType.TEXT_PLAIN_VALUE,
        "# playbook".getBytes()
    );

    mockMvc.perform(multipart("/api/organizations/acme/workflow-items/{workflowItemId}/attachments", workflowItemId)
            .file(file)
            .with(csrf())
            .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(attachmentId.toString()))
        .andExpect(jsonPath("$.fileName").value("playbook.md"))
        .andExpect(jsonPath("$.contentType").value("text/markdown"));
  }

  @Test
  void listAttachments_returnsAttachments() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID attachmentId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID workspaceId = UUID.randomUUID();

    UserAccount user = activeUser(userId);
    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(attachmentService.listAttachments(any(), any(), any())).thenReturn(java.util.List.of(
        new AttachmentResponse(
            attachmentId,
            workflowItemId,
            organizationId,
            workspaceId,
            userId,
            "playbook.md",
            "text/markdown",
            123L,
            "LOCAL",
            "acme/workflow/playbook.md",
            "checksum",
            OffsetDateTime.now(ZoneOffset.UTC),
            null
        )
    ));

    mockMvc.perform(get("/api/organizations/acme/workflow-items/{workflowItemId}/attachments", workflowItemId)
            .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(attachmentId.toString()))
        .andExpect(jsonPath("$[0].fileName").value("playbook.md"));
  }

  @Test
  void downloadAttachment_returnsBinaryResponse() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID attachmentId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID workspaceId = UUID.randomUUID();

    UserAccount user = activeUser(userId);
    AttachmentResponse attachmentResponse = new AttachmentResponse(
        attachmentId,
        workflowItemId,
        organizationId,
        workspaceId,
        userId,
        "playbook.md",
        "text/markdown",
        123L,
        "LOCAL",
        "acme/workflow/playbook.md",
        "checksum",
        OffsetDateTime.now(ZoneOffset.UTC),
        null
    );

    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(attachmentService.getAttachment(any(), any(), any(), any())).thenReturn(attachmentResponse);
    when(attachmentService.downloadAttachment(any(), any(), any(), any())).thenReturn(
        new InputStreamResource(new ByteArrayInputStream("demo".getBytes()))
    );

    mockMvc.perform(get("/api/organizations/acme/workflow-items/{workflowItemId}/attachments/{attachmentId}/download", workflowItemId, attachmentId)
            .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/markdown")))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, Matchers.containsString("playbook.md")));
  }

  @Test
  void deleteAttachment_returnsNoContent() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID attachmentId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();

    UserAccount user = activeUser(userId);
    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));

    mockMvc.perform(delete("/api/organizations/acme/workflow-items/{workflowItemId}/attachments/{attachmentId}", workflowItemId, attachmentId)
            .with(csrf())
            .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isNoContent());
  }

  private UserAccount activeUser(UUID userId) {
    UserAccount user = new UserAccount();
    user.setId(userId);
    user.setEmail("nhat@example.com");
    user.setFullName("Nhat");
    user.setPasswordHash("hashed");
    user.setStatus(UserStatus.ACTIVE);
    user.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    user.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    return user;
  }
}
