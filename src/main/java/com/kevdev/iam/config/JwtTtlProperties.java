package com.kevdev.iam.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ias.jwt")
public record JwtTtlProperties(
        Duration accessTtl,
        Duration refreshTtl
) {}

