package com.nhat.workflowhub.workspace.repository;

import com.nhat.workflowhub.workspace.entity.Workspace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

  List<Workspace> findAllByOrganizationId(UUID organizationId);

  Optional<Workspace> findByOrganizationIdAndSlug(UUID organizationId, String slug);

  boolean existsByOrganizationIdAndSlug(UUID organizationId, String slug);
}
