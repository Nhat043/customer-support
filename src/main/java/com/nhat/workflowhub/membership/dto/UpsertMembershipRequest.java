package com.nhat.workflowhub.membership.dto;

import com.nhat.workflowhub.auth.entity.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpsertMembershipRequest(
    @Email @NotBlank String email,
    @NotNull UserRole role,
    String workspaceSlug
) {
}
