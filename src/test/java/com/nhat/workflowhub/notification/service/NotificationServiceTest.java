package com.nhat.workflowhub.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nhat.workflowhub.membership.entity.Membership;
import com.nhat.workflowhub.membership.repository.MembershipRepository;
import com.nhat.workflowhub.notification.entity.Notification;
import com.nhat.workflowhub.notification.repository.NotificationRepository;
import com.nhat.workflowhub.organization.entity.Organization;
import com.nhat.workflowhub.organization.service.OrganizationService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private MembershipRepository membershipRepository;
  @Mock
  private OrganizationService organizationService;

  @Test
  void notifyWorkflowItemEvent_createsNotificationsForEligibleMembersOnly() {
    UUID organizationId = UUID.randomUUID();
    UUID workspaceId = UUID.randomUUID();
    UUID actorUserId = UUID.randomUUID();
    UUID recipientUserId = UUID.randomUUID();

    Organization organization = new Organization();
    organization.setId(organizationId);
    organization.setSlug("acme");
    organization.setName("Acme");
    organization.setOwnerUserId(actorUserId);

    Membership actorMembership = membership(organizationId, null, actorUserId);
    Membership recipientMembership = membership(organizationId, workspaceId, recipientUserId);

    when(membershipRepository.findAllByOrganizationId(organizationId)).thenReturn(List.of(actorMembership, recipientMembership));
    when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

    NotificationService service = new NotificationService(notificationRepository, membershipRepository, organizationService);
    service.notifyWorkflowItemEvent(
        organization,
        workspaceId,
        UUID.randomUUID(),
        actorUserId,
        "WORKFLOW_ITEM_CREATED",
        "New request: Payment failed",
        "A new workflow item was created in the workspace."
    );

    ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(notificationCaptor.capture());
    Notification notification = notificationCaptor.getValue();
    assertThat(notification.getUserId()).isEqualTo(recipientUserId);
    assertThat(notification.getType()).isEqualTo("WORKFLOW_ITEM_CREATED");
    assertThat(notification.getEntityType()).isEqualTo("workflow_item");
  }

  @Test
  void markAsRead_updatesOnlyTheCurrentUsersNotification() {
    UUID organizationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID notificationId = UUID.randomUUID();

    Organization organization = new Organization();
    organization.setId(organizationId);
    organization.setSlug("acme");
    organization.setName("Acme");
    organization.setOwnerUserId(userId);

    Notification notification = new Notification();
    notification.setId(notificationId);
    notification.setOrganizationId(organizationId);
    notification.setWorkspaceId(UUID.randomUUID());
    notification.setUserId(userId);
    notification.setType("COMMENT_ADDED");
    notification.setTitle("New comment");
    notification.setBody("Hello");
    notification.setEntityType("comment");
    notification.setEntityId(UUID.randomUUID());
    notification.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));

    when(organizationService.requireAccessibleOrganization("acme", userId)).thenReturn(organization);
    when(notificationRepository.findByIdAndOrganizationIdAndUserId(notificationId, organizationId, userId))
        .thenReturn(Optional.of(notification));
    when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

    NotificationService service = new NotificationService(notificationRepository, membershipRepository, organizationService);
    var response = service.markAsRead("acme", notificationId, userId);

    assertThat(response.readAt()).isNotNull();
    verify(notificationRepository).save(notification);
  }

  private Membership membership(UUID organizationId, UUID workspaceId, UUID userId) {
    Membership membership = new Membership();
    membership.setId(UUID.randomUUID());
    membership.setOrganizationId(organizationId);
    membership.setWorkspaceId(workspaceId);
    membership.setUserId(userId);
    membership.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    membership.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    return membership;
  }
}
