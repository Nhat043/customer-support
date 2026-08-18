package com.nhat.workflowhub.comment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nhat.workflowhub.auth.entity.UserAccount;
import com.nhat.workflowhub.auth.entity.UserStatus;
import com.nhat.workflowhub.auth.repository.UserAccountRepository;
import com.nhat.workflowhub.auth.security.JwtService;
import com.nhat.workflowhub.comment.dto.CommentResponse;
import com.nhat.workflowhub.comment.entity.Comment;
import com.nhat.workflowhub.comment.service.CommentService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CommentControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtService jwtService;
  @MockBean
  private UserAccountRepository userAccountRepository;
  @MockBean
  private CommentService commentService;

  @Test
  void addComment_returnsCreatedComment() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
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
    when(commentService.addComment(any(), any(), any(), any())).thenReturn(
        new CommentResponse(
            commentId,
            workflowItemId,
            organizationId,
            workspaceId,
            userId,
            "hello world",
            OffsetDateTime.now(ZoneOffset.UTC),
            OffsetDateTime.now(ZoneOffset.UTC)
        )
    );

    mockMvc.perform(post("/api/organizations/acme/workflow-items/{workflowItemId}/comments", workflowItemId)
            .with(csrf())
            .header("Authorization", "Bearer valid-token")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"body":" hello world "}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(commentId.toString()))
        .andExpect(jsonPath("$.body").value("hello world"))
        .andExpect(jsonPath("$.workflowItemId").value(workflowItemId.toString()));
  }

  @Test
  void listComments_returnsComments() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();
    UUID organizationId = UUID.randomUUID();
    UUID workspaceId = UUID.randomUUID();

    UserAccount user = activeUser(userId);
    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(commentService.listComments(any(), any(), any())).thenReturn(List.of(
        new CommentResponse(
            commentId,
            workflowItemId,
            organizationId,
            workspaceId,
            userId,
            "hello world",
            OffsetDateTime.now(ZoneOffset.UTC),
            OffsetDateTime.now(ZoneOffset.UTC)
        )
    ));

    mockMvc.perform(get("/api/organizations/acme/workflow-items/{workflowItemId}/comments", workflowItemId)
            .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(commentId.toString()))
        .andExpect(jsonPath("$[0].body").value("hello world"));
  }

  @Test
  void deleteComment_returnsNoContent() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID workflowItemId = UUID.randomUUID();
    UUID commentId = UUID.randomUUID();

    UserAccount user = activeUser(userId);
    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));

    mockMvc.perform(delete("/api/organizations/acme/workflow-items/{workflowItemId}/comments/{commentId}", workflowItemId, commentId)
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
