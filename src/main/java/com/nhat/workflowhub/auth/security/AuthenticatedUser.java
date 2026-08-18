package com.nhat.workflowhub.auth.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String email, String fullName) {
}
