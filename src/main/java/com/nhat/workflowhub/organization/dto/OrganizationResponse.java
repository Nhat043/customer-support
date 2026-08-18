package com.nhat.workflowhub.organization.dto;

import java.util.UUID;

public record OrganizationResponse(UUID id, String name, String slug, UUID ownerUserId) {
}
