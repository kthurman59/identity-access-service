package com.kevdev.iam.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record SecurityProperties(
    String issuer,
    String secret,
    long accessTokenTtlSeconds,
    long refreshTokenTtlSeconds
) {
  public SecurityProperties {
    if (accessTokenTtlSeconds <= 0) accessTokenTtlSeconds = 900;
    if (refreshTokenTtlSeconds <= 0) refreshTokenTtlSeconds = 604800; // 7 days
  }
}

