package com.kevdev.iam.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtTtlProperties.class)
public class SecurityTtlConfig {

    @Bean
    @Qualifier("accessTokenTtl")
    public Duration accessTokenTtl(JwtTtlProperties props) {
        return props.accessTtl();
    }

    @Bean
    @Qualifier("refreshTokenTtl")
    public Duration refreshTokenTtl(JwtTtlProperties props) {
        return props.refreshTtl();
    }
}

