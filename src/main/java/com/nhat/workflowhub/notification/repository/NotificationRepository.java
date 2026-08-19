package com.nhat.workflowhub.notification.repository;

import com.nhat.workflowhub.notification.entity.Notification;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  List<Notification> findAllByOrganizationIdAndUserIdOrderByCreatedAtDesc(UUID organizationId, UUID userId);

  List<Notification> findAllByOrganizationIdAndUserIdAndReadAtIsNullOrderByCreatedAtDesc(UUID organizationId, UUID userId);

  Optional<Notification> findByIdAndOrganizationIdAndUserId(UUID id, UUID organizationId, UUID userId);

  long countByOrganizationIdAndUserIdAndReadAtIsNull(UUID organizationId, UUID userId);
}
