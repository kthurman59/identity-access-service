package com.kevdev.iam.security;

import com.kevdev.iam.repo.RefreshTokenRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(prefix = "ias.tasks", name = "scheduling-enabled", havingValue = "true", matchIfMissing = false)
public class RefreshTokenCleanupJob {

  private final RefreshTokenRepository refreshTokenRepository;

  public RefreshTokenCleanupJob(RefreshTokenRepository refreshTokenRepository) {
    this.refreshTokenRepository = refreshTokenRepository;
  }

  // daily cleanup in dev or prod when enabled
  @Scheduled(cron = "0 0 3 * * *")
  public void purge() {
    refreshTokenRepository.deleteExpiredOrRevoked(Instant.now());
  }
}

