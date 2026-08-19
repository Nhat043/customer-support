package com.nhat.workflowhub.notification.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nhat.workflowhub.auth.entity.UserAccount;
import com.nhat.workflowhub.auth.entity.UserStatus;
import com.nhat.workflowhub.auth.repository.UserAccountRepository;
import com.nhat.workflowhub.auth.security.JwtService;
import com.nhat.workflowhub.notification.dto.NotificationResponse;
import com.nhat.workflowhub.notification.service.NotificationService;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private JwtService jwtService;
  @MockBean
  private UserAccountRepository userAccountRepository;
  @MockBean
  private NotificationService notificationService;

  @Test
  void listNotifications_returnsNotifications() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();

    UserAccount user = activeUser(userId);
    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(notificationService.listNotifications("acme", userId)).thenReturn(List.of(
        new NotificationResponse(
            notificationId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            userId,
            "WORKFLOW_ITEM_CREATED",
            "New request",
            "A request was created",
            "workflow_item",
            UUID.randomUUID(),
            null,
            OffsetDateTime.now(ZoneOffset.UTC)
        )
    ));

    mockMvc.perform(get("/api/organizations/acme/notifications")
            .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(notificationId.toString()))
        .andExpect(jsonPath("$[0].type").value("WORKFLOW_ITEM_CREATED"));
  }

  @Test
  void unreadCount_returnsBadgeCount() throws Exception {
    UUID userId = UUID.randomUUID();

    UserAccount user = activeUser(userId);
    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(notificationService.unreadCount("acme", userId)).thenReturn(3L);

    mockMvc.perform(get("/api/organizations/acme/notifications/unread-count")
            .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.unreadCount").value(3));
  }

  @Test
  void markNotificationRead_returnsUpdatedNotification() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();

    UserAccount user = activeUser(userId);
    when(jwtService.isValidAccessToken("valid-token")).thenReturn(true);
    when(jwtService.extractUserId("valid-token")).thenReturn(userId);
    when(userAccountRepository.findById(userId)).thenReturn(Optional.of(user));
    when(notificationService.markAsRead(any(), any(), any())).thenReturn(
        new NotificationResponse(
            notificationId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            userId,
            "WORKFLOW_ITEM_UPDATED",
            "Request updated",
            "The request changed",
            "workflow_item",
            UUID.randomUUID(),
            OffsetDateTime.now(ZoneOffset.UTC),
            OffsetDateTime.now(ZoneOffset.UTC)
        )
    );

    mockMvc.perform(patch("/api/organizations/acme/notifications/{notificationId}/read", notificationId)
            .with(csrf())
            .header("Authorization", "Bearer valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(notificationId.toString()))
        .andExpect(jsonPath("$.readAt").isNotEmpty());
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
