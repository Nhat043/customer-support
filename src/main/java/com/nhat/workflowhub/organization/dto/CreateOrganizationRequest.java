package com.nhat.workflowhub.organization.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateOrganizationRequest(
    @NotBlank String name,
    String slug
) {
}
