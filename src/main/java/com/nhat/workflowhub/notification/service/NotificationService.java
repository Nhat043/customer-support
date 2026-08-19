package com.nhat.workflowhub.notification.service;

import com.nhat.workflowhub.common.exception.ApiException;
import com.nhat.workflowhub.membership.entity.Membership;
import com.nhat.workflowhub.membership.repository.MembershipRepository;
import com.nhat.workflowhub.notification.dto.NotificationResponse;
import com.nhat.workflowhub.notification.entity.Notification;
import com.nhat.workflowhub.notification.repository.NotificationRepository;
import com.nhat.workflowhub.organization.entity.Organization;
import com.nhat.workflowhub.organization.service.OrganizationService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationService {

  private final NotificationRepository notificationRepository;
  private final MembershipRepository membershipRepository;
  private final OrganizationService organizationService;

  public NotificationService(
      NotificationRepository notificationRepository,
      MembershipRepository membershipRepository,
      OrganizationService organizationService
  ) {
    this.notificationRepository = notificationRepository;
    this.membershipRepository = membershipRepository;
    this.organizationService = organizationService;
  }

  @Transactional(readOnly = true)
  public List<NotificationResponse> listNotifications(String organizationSlug, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    return notificationRepository.findAllByOrganizationIdAndUserIdOrderByCreatedAtDesc(organization.getId(), currentUserId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public long unreadCount(String organizationSlug, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    return notificationRepository.countByOrganizationIdAndUserIdAndReadAtIsNull(organization.getId(), currentUserId);
  }

  public NotificationResponse markAsRead(String organizationSlug, UUID notificationId, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    Notification notification = notificationRepository
        .findByIdAndOrganizationIdAndUserId(notificationId, organization.getId(), currentUserId)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
    if (notification.getReadAt() == null) {
      notification.setReadAt(OffsetDateTime.now(ZoneOffset.UTC));
      notificationRepository.save(notification);
    }
    return toResponse(notification);
  }

  public long markAllAsRead(String organizationSlug, UUID currentUserId) {
    Organization organization = organizationService.requireAccessibleOrganization(organizationSlug, currentUserId);
    List<Notification> unreadNotifications = notificationRepository
        .findAllByOrganizationIdAndUserIdAndReadAtIsNullOrderByCreatedAtDesc(organization.getId(), currentUserId);
    unreadNotifications.forEach(notification -> notification.setReadAt(OffsetDateTime.now(ZoneOffset.UTC)));
    notificationRepository.saveAll(unreadNotifications);
    return unreadNotifications.size();
  }

  public void notifyWorkflowItemEvent(
      Organization organization,
      UUID workspaceId,
      UUID entityId,
      UUID actorUserId,
      String type,
      String title,
      String body
  ) {
    notifyOrganizationMembers(organization, workspaceId, entityId, "workflow_item", actorUserId, type, title, body);
  }

  public void notifyCommentEvent(
      Organization organization,
      UUID workspaceId,
      UUID entityId,
      UUID actorUserId,
      String type,
      String title,
      String body
  ) {
    notifyOrganizationMembers(organization, workspaceId, entityId, "comment", actorUserId, type, title, body);
  }

  public void notifyAttachmentEvent(
      Organization organization,
      UUID workspaceId,
      UUID entityId,
      UUID actorUserId,
      String type,
      String title,
      String body
  ) {
    notifyOrganizationMembers(organization, workspaceId, entityId, "attachment", actorUserId, type, title, body);
  }

  public NotificationResponse toResponse(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getOrganizationId(),
        notification.getWorkspaceId(),
        notification.getUserId(),
        notification.getType(),
        notification.getTitle(),
        notification.getBody(),
        notification.getEntityType(),
        notification.getEntityId(),
        notification.getReadAt(),
        notification.getCreatedAt()
    );
  }

  private void notifyOrganizationMembers(
      Organization organization,
      UUID workspaceId,
      UUID entityId,
      String entityType,
      UUID actorUserId,
      String type,
      String title,
      String body
  ) {
    Set<UUID> recipientIds = new LinkedHashSet<>();
    for (Membership membership : membershipRepository.findAllByOrganizationId(organization.getId())) {
      if (membership.getWorkspaceId() == null || workspaceId == null || workspaceId.equals(membership.getWorkspaceId())) {
        recipientIds.add(membership.getUserId());
      }
    }

    recipientIds.remove(actorUserId);
    for (UUID recipientId : recipientIds) {
      Notification notification = new Notification();
      notification.setId(UUID.randomUUID());
      notification.setOrganizationId(organization.getId());
      notification.setWorkspaceId(workspaceId);
      notification.setUserId(recipientId);
      notification.setType(type);
      notification.setTitle(title);
      notification.setBody(body);
      notification.setEntityType(entityType);
      notification.setEntityId(entityId);
      notification.setReadAt(null);
      notificationRepository.save(notification);
    }
  }
}
