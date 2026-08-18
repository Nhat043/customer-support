package com.nhat.workflowhub.comment.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCommentRequest(
    @NotBlank String body
) {
}
