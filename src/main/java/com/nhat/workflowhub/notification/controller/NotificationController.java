package com.nhat.workflowhub.notification.controller;

import com.nhat.workflowhub.auth.security.AuthenticatedUser;
import com.nhat.workflowhub.notification.dto.NotificationResponse;
import com.nhat.workflowhub.notification.service.NotificationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{orgSlug}/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  public List<NotificationResponse> list(
      @PathVariable("orgSlug") String orgSlug,
      Authentication authentication
  ) {
    return notificationService.listNotifications(orgSlug, currentUserId(authentication));
  }

  @GetMapping("/unread-count")
  public ResponseEntity<Map<String, Object>> unreadCount(
      @PathVariable("orgSlug") String orgSlug,
      Authentication authentication
  ) {
    long unreadCount = notificationService.unreadCount(orgSlug, currentUserId(authentication));
    return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
  }

  @PatchMapping("/{notificationId}/read")
  public NotificationResponse markAsRead(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("notificationId") UUID notificationId,
      Authentication authentication
  ) {
    return notificationService.markAsRead(orgSlug, notificationId, currentUserId(authentication));
  }

  @PatchMapping("/read-all")
  public ResponseEntity<Map<String, Object>> markAllAsRead(
      @PathVariable("orgSlug") String orgSlug,
      Authentication authentication
  ) {
    long updated = notificationService.markAllAsRead(orgSlug, currentUserId(authentication));
    return ResponseEntity.ok(Map.of("updated", updated));
  }

  private UUID currentUserId(Authentication authentication) {
    return ((AuthenticatedUser) authentication.getPrincipal()).userId();
  }
}
