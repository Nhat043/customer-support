package com.nhat.workflowhub.workspace.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkspaceRequest(
    @NotBlank String name,
    String slug
) {
}
