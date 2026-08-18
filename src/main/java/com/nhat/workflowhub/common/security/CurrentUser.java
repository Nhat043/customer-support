package com.nhat.workflowhub.common.security;

public record CurrentUser(Long userId, String email, String role) {
}
