package com.nhat.workflowhub.workflow.repository;

import com.nhat.workflowhub.workflow.entity.WorkflowItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowItemRepository extends JpaRepository<WorkflowItem, UUID> {

  List<WorkflowItem> findAllByOrganizationIdOrderByUpdatedAtDesc(UUID organizationId);

  List<WorkflowItem> findAllByOrganizationIdAndWorkspaceIdOrderByUpdatedAtDesc(UUID organizationId, UUID workspaceId);

  java.util.Optional<WorkflowItem> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
