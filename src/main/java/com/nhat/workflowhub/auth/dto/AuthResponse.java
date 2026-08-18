package com.nhat.workflowhub.auth.dto;

import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String tokenType,
    UUID userId,
    String email,
    String fullName
) {
}
