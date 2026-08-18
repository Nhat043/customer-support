package com.nhat.workflowhub.membership.repository;

import com.nhat.workflowhub.membership.entity.Membership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {

  List<Membership> findAllByOrganizationId(UUID organizationId);

  List<Membership> findAllByOrganizationIdAndUserId(UUID organizationId, UUID userId);

  List<Membership> findAllByOrganizationIdAndWorkspaceId(UUID organizationId, UUID workspaceId);

  List<Membership> findAllByUserId(UUID userId);

  Optional<Membership> findByOrganizationIdAndWorkspaceIdAndUserId(
      UUID organizationId,
      UUID workspaceId,
      UUID userId
  );

  Optional<Membership> findByOrganizationIdAndWorkspaceIdIsNullAndUserId(UUID organizationId, UUID userId);

  boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}
