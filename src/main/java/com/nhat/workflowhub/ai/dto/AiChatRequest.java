package com.nhat.workflowhub.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
    String organizationSlug,
    String workspaceSlug,
    @NotBlank String message
) {
}
