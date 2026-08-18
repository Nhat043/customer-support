package com.nhat.workflowhub.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

  private final Jwt jwt = new Jwt();
  private final Cookie cookie = new Cookie();

  public Jwt getJwt() {
    return jwt;
  }

  public Cookie getCookie() {
    return cookie;
  }

  public static class Jwt {
    private String issuer;
    private String secret;
    private Duration accessTokenTtl = Duration.ofMinutes(15);
    private Duration refreshTokenTtl = Duration.ofDays(30);

    public String getIssuer() {
      return issuer;
    }

    public void setIssuer(String issuer) {
      this.issuer = issuer;
    }

    public String getSecret() {
      return secret;
    }

    public void setSecret(String secret) {
      this.secret = secret;
    }

    public Duration getAccessTokenTtl() {
      return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
      this.accessTokenTtl = accessTokenTtl;
    }

    public Duration getRefreshTokenTtl() {
      return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
      this.refreshTokenTtl = refreshTokenTtl;
    }
  }

  public static class Cookie {
    private String refreshTokenName = "customer_support_refresh";
    private boolean secure;
    private String domain;

    public String getRefreshTokenName() {
      return refreshTokenName;
    }

    public void setRefreshTokenName(String refreshTokenName) {
      this.refreshTokenName = refreshTokenName;
    }

    public boolean isSecure() {
      return secure;
    }

    public void setSecure(boolean secure) {
      this.secure = secure;
    }

    public String getDomain() {
      return domain;
    }

    public void setDomain(String domain) {
      this.domain = domain;
    }
  }
}
