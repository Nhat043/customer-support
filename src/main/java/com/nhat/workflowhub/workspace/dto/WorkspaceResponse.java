package com.nhat.workflowhub.workspace.dto;

import java.util.UUID;

public record WorkspaceResponse(
    UUID id,
    UUID organizationId,
    String name,
    String slug
) {
}
