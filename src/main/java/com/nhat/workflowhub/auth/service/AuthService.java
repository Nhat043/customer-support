package com.nhat.workflowhub.auth.service;

import com.nhat.workflowhub.auth.dto.AuthResponse;
import com.nhat.workflowhub.auth.dto.LoginRequest;
import com.nhat.workflowhub.auth.dto.RegisterRequest;
import com.nhat.workflowhub.auth.entity.RefreshSession;
import com.nhat.workflowhub.auth.entity.UserAccount;
import com.nhat.workflowhub.auth.entity.UserStatus;
import com.nhat.workflowhub.auth.repository.RefreshSessionRepository;
import com.nhat.workflowhub.auth.repository.UserAccountRepository;
import com.nhat.workflowhub.auth.security.JwtService;
import com.nhat.workflowhub.common.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

  private final UserAccountRepository userAccountRepository;
  private final RefreshSessionRepository refreshSessionRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserAccountRepository userAccountRepository,
      RefreshSessionRepository refreshSessionRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService
  ) {
    this.userAccountRepository = userAccountRepository;
    this.refreshSessionRepository = refreshSessionRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public AuthResult register(RegisterRequest request, HttpServletRequest httpRequest) {
    if (userAccountRepository.existsByEmail(request.email())) {
      throw new ApiException(HttpStatus.CONFLICT, "Email already exists");
    }

    UserAccount user = new UserAccount();
    user.setId(UUID.randomUUID());
    user.setEmail(request.email().toLowerCase());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setFullName(request.fullName());
    user.setStatus(UserStatus.ACTIVE);
    user.setCreatedAt(jwtService.now());
    user.setUpdatedAt(jwtService.now());
    userAccountRepository.save(user);

    return issueTokens(user, httpRequest);
  }

  public AuthResult login(LoginRequest request, HttpServletRequest httpRequest) {
    UserAccount user = userAccountRepository.findByEmail(request.email().toLowerCase())
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

    if (user.getStatus() != UserStatus.ACTIVE) {
      throw new ApiException(HttpStatus.FORBIDDEN, "User is not active");
    }

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    return issueTokens(user, httpRequest);
  }

  public AuthResult refresh(String refreshToken, HttpServletRequest httpRequest) {
    RefreshSession session = findActiveSession(refreshToken);
    UserAccount user = userAccountRepository.findById(session.getUserId())
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

    revokeSession(session);
    return issueTokens(user, httpRequest);
  }

  public void logout(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      return;
    }

    refreshSessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(jwtService.hashRefreshToken(refreshToken))
        .ifPresent(this::revokeSession);
  }

  private AuthResult issueTokens(UserAccount user, HttpServletRequest httpRequest) {
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken();
    saveSession(user, refreshToken, httpRequest);
    AuthResponse response = new AuthResponse(
        accessToken,
        "Bearer",
        user.getId(),
        user.getEmail(),
        user.getFullName()
    );
    return new AuthResult(response, refreshToken);
  }

  private void saveSession(UserAccount user, String refreshToken, HttpServletRequest httpRequest) {
    RefreshSession session = new RefreshSession();
    session.setId(UUID.randomUUID());
    session.setUserId(user.getId());
    session.setRefreshTokenHash(jwtService.hashRefreshToken(refreshToken));
    session.setDeviceName(resolveDeviceName(httpRequest));
    session.setIpAddress(resolveIpAddress(httpRequest));
    session.setUserAgent(httpRequest.getHeader("User-Agent"));
    session.setRevokedAt(null);
    session.setExpiresAt(jwtService.refreshExpiresAt());
    session.setCreatedAt(jwtService.now());
    refreshSessionRepository.save(session);
  }

  private RefreshSession findActiveSession(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token is missing");
    }

    RefreshSession session = refreshSessionRepository
        .findByRefreshTokenHashAndRevokedAtIsNull(jwtService.hashRefreshToken(refreshToken))
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

    if (session.getExpiresAt().isBefore(jwtService.now())) {
      revokeSession(session);
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
    }

    return session;
  }

  private void revokeSession(RefreshSession session) {
    session.setRevokedAt(jwtService.now());
    refreshSessionRepository.save(session);
  }

  private String resolveIpAddress(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }

  private String resolveDeviceName(HttpServletRequest request) {
    String userAgent = request.getHeader("User-Agent");
    if (userAgent == null || userAgent.isBlank()) {
      return "unknown";
    }
    return userAgent.length() > 255 ? userAgent.substring(0, 255) : userAgent;
  }
}
