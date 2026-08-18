package com.nhat.workflowhub.membership.dto;

import com.nhat.workflowhub.auth.entity.UserRole;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MembershipResponse(
    UUID id,
    UUID organizationId,
    UUID workspaceId,
    UUID userId,
    String email,
    String fullName,
    UserRole role,
    OffsetDateTime createdAt
) {
}
