package com.kevdev.iam.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthBeans {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public Duration refreshTokenTtl(@Value("${ias.refresh.ttl}") Duration ttl) {
    return ttl;
  }
}

