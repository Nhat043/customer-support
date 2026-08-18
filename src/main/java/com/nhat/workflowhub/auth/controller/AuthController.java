package com.nhat.workflowhub.auth.controller;

import com.nhat.workflowhub.auth.dto.AuthResponse;
import com.nhat.workflowhub.auth.dto.LoginRequest;
import com.nhat.workflowhub.auth.dto.RegisterRequest;
import com.nhat.workflowhub.auth.security.AuthenticatedUser;
import com.nhat.workflowhub.auth.service.AuthResult;
import com.nhat.workflowhub.auth.service.AuthService;
import com.nhat.workflowhub.config.AuthProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final AuthProperties authProperties;

  public AuthController(AuthService authService, AuthProperties authProperties) {
    this.authService = authService;
    this.authProperties = authProperties;
  }

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(
      @Valid @RequestBody RegisterRequest request,
      HttpServletRequest httpRequest
  ) {
    AuthResult result = authService.register(request, httpRequest);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshToken()))
        .body(result.response());
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest
  ) {
    AuthResult result = authService.login(request, httpRequest);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshToken()))
        .body(result.response());
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(
      HttpServletRequest httpRequest
  ) {
    String refreshToken = readCookie(httpRequest, authProperties.getCookie().getRefreshTokenName());
    AuthResult result = authService.refresh(refreshToken, httpRequest);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshToken()))
        .body(result.response());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
    String refreshToken = readCookie(httpRequest, authProperties.getCookie().getRefreshTokenName());
    authService.logout(refreshToken);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, clearRefreshCookie())
        .build();
  }

  @GetMapping("/me")
  public AuthenticatedUser me(Authentication authentication) {
    return (AuthenticatedUser) authentication.getPrincipal();
  }

  private String buildRefreshCookie(String refreshToken) {
    ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(
        authProperties.getCookie().getRefreshTokenName(),
        refreshToken
    )
        .httpOnly(true)
        .secure(authProperties.getCookie().isSecure())
        .path("/api/auth")
        .sameSite("Lax")
        .maxAge(authProperties.getJwt().getRefreshTokenTtl());

    if (authProperties.getCookie().getDomain() != null && !authProperties.getCookie().getDomain().isBlank()) {
      builder.domain(authProperties.getCookie().getDomain());
    }

    return builder.build().toString();
  }

  private String clearRefreshCookie() {
    ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(
        authProperties.getCookie().getRefreshTokenName(),
        ""
    )
        .httpOnly(true)
        .secure(authProperties.getCookie().isSecure())
        .path("/api/auth")
        .sameSite("Lax")
        .maxAge(0);

    if (authProperties.getCookie().getDomain() != null && !authProperties.getCookie().getDomain().isBlank()) {
      builder.domain(authProperties.getCookie().getDomain());
    }

    return builder.build().toString();
  }

  private String readCookie(HttpServletRequest request, String cookieName) {
    if (request.getCookies() == null) {
      return null;
    }
    for (var cookie : request.getCookies()) {
      if (cookieName.equals(cookie.getName())) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
