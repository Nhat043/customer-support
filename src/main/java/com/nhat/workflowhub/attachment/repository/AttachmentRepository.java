package com.nhat.workflowhub.attachment.repository;

import com.nhat.workflowhub.attachment.entity.Attachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

  List<Attachment> findAllByWorkflowItemIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID workflowItemId);

  java.util.Optional<Attachment> findByIdAndWorkflowItemId(UUID id, UUID workflowItemId);

  java.util.Optional<Attachment> findByIdAndWorkflowItemIdAndDeletedAtIsNull(UUID id, UUID workflowItemId);
}
