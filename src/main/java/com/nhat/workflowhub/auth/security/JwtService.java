package com.nhat.workflowhub.auth.security;

import com.nhat.workflowhub.auth.entity.UserAccount;
import com.nhat.workflowhub.config.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  private final AuthProperties authProperties;
  private final Clock clock = Clock.systemUTC();
  private final SecretKey signingKey;

  public JwtService(AuthProperties authProperties) {
    this.authProperties = authProperties;
    this.signingKey = Keys.hmacShaKeyFor(authProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
  }

  public String generateAccessToken(UserAccount user) {
    Instant now = clock.instant();
    Instant expiresAt = now.plus(authProperties.getJwt().getAccessTokenTtl());
    return Jwts.builder()
        .issuer(authProperties.getJwt().getIssuer())
        .subject(user.getId().toString())
        .claim("email", user.getEmail())
        .claim("fullName", user.getFullName())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiresAt))
        .signWith(signingKey)
        .compact();
  }

  public UUID extractUserId(String token) {
    Claims claims = parseClaims(token);
    return UUID.fromString(claims.getSubject());
  }

  public boolean isValidAccessToken(String token) {
    try {
      parseClaims(token);
      return true;
    } catch (Exception exception) {
      return false;
    }
  }

  public String generateRefreshToken() {
    byte[] random = new byte[32];
    SECURE_RANDOM.nextBytes(random);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(random);
  }

  public String hashRefreshToken(String refreshToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
      return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to hash refresh token", exception);
    }
  }

  public OffsetDateTime now() {
    return OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
  }

  public OffsetDateTime refreshExpiresAt() {
    return now().plus(authProperties.getJwt().getRefreshTokenTtl());
  }

  private Claims parseClaims(String token) {
    return Jwts.parser()
        .verifyWith(signingKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
