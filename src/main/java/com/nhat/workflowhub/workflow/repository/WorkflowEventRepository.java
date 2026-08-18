package com.nhat.workflowhub.workflow.repository;

import com.nhat.workflowhub.workflow.entity.WorkflowEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowEventRepository extends JpaRepository<WorkflowEvent, UUID> {

  List<WorkflowEvent> findAllByWorkflowItemIdOrderByCreatedAtDesc(UUID workflowItemId);
}
