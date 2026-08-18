package com.nhat.workflowhub.auth.service;

import com.nhat.workflowhub.auth.dto.AuthResponse;

public record AuthResult(AuthResponse response, String refreshToken) {
}

