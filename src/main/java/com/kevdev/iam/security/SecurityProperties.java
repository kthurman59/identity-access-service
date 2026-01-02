package com.kevdev.iam.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.jwt")
public record SecurityProperties(
        String issuer,
        String secret,
        long accessTokenTtlSeconds
) {
}

