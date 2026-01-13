package com.kevdev.iam.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Component
public class RefreshTokenCleanupJob {

  private final JdbcTemplate jdbc;
  private final Clock clock;

  public RefreshTokenCleanupJob(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = Objects.requireNonNull(jdbc);
    this.clock = Objects.requireNonNull(clock);
  }

  public int cleanupExpired() {
    Instant now = Instant.now(clock);
    return jdbc.update("DELETE FROM refresh_token WHERE expires_at IS NOT NULL AND expires_at < ?", now);
  }
}

