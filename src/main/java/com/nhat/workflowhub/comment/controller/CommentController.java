package com.nhat.workflowhub.comment.controller;

import com.nhat.workflowhub.auth.security.AuthenticatedUser;
import com.nhat.workflowhub.comment.dto.CommentResponse;
import com.nhat.workflowhub.comment.dto.CreateCommentRequest;
import com.nhat.workflowhub.comment.dto.UpdateCommentRequest;
import com.nhat.workflowhub.comment.service.CommentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organizations/{orgSlug}/workflow-items/{workflowItemId}/comments")
public class CommentController {

  private final CommentService commentService;

  public CommentController(CommentService commentService) {
    this.commentService = commentService;
  }

  @GetMapping
  public List<CommentResponse> list(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("workflowItemId") UUID workflowItemId,
      Authentication authentication
  ) {
    return commentService.listComments(orgSlug, workflowItemId, currentUserId(authentication));
  }

  @PostMapping
  public CommentResponse add(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("workflowItemId") UUID workflowItemId,
      @Valid @RequestBody CreateCommentRequest request,
      Authentication authentication
  ) {
    return commentService.addComment(orgSlug, workflowItemId, request, currentUserId(authentication));
  }

  @PatchMapping("/{commentId}")
  public CommentResponse update(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("workflowItemId") UUID workflowItemId,
      @PathVariable("commentId") UUID commentId,
      @Valid @RequestBody UpdateCommentRequest request,
      Authentication authentication
  ) {
    return commentService.updateComment(orgSlug, workflowItemId, commentId, request, currentUserId(authentication));
  }

  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> delete(
      @PathVariable("orgSlug") String orgSlug,
      @PathVariable("workflowItemId") UUID workflowItemId,
      @PathVariable("commentId") UUID commentId,
      Authentication authentication
  ) {
    commentService.deleteComment(orgSlug, workflowItemId, commentId, currentUserId(authentication));
    return ResponseEntity.noContent().build();
  }

  private UUID currentUserId(Authentication authentication) {
    return ((AuthenticatedUser) authentication.getPrincipal()).userId();
  }
}
