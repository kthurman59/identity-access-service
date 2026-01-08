package com.kevdev.iam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ias.jwt")
public class JwtConfig {

  /**
   * Optional issuer override.
   * If unset, JwtTokenService can fall back to its own default.
   */
  private String issuer;

  /**
   * Access token TTL in minutes.
   */
  private long accessTtlMinutes = 15;

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public long getAccessTtlMinutes() {
    return accessTtlMinutes;
  }

  public void setAccessTtlMinutes(long accessTtlMinutes) {
    this.accessTtlMinutes = accessTtlMinutes;
  }
}

