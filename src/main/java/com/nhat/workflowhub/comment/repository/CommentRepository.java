package com.nhat.workflowhub.comment.repository;

import com.nhat.workflowhub.comment.entity.Comment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  List<Comment> findAllByWorkflowItemIdOrderByCreatedAtAsc(UUID workflowItemId);

  java.util.Optional<Comment> findByIdAndWorkflowItemId(UUID id, UUID workflowItemId);
}
