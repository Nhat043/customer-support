package com.nhat.workflowhub.organization.repository;

import com.nhat.workflowhub.organization.entity.Organization;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

  Optional<Organization> findBySlug(String slug);

  List<Organization> findAllByOwnerUserId(UUID ownerUserId);

  boolean existsBySlug(String slug);
}
