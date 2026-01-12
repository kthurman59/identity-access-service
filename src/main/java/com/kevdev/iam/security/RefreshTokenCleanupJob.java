package com.kevdev.iam.security;

import com.kevdev.iam.repo.RefreshTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RefreshTokenCleanupJob {

  private final RefreshTokenRepository repo;

  public RefreshTokenCleanupJob(RefreshTokenRepository repo) {
    this.repo = repo;
  }

  // runs every hour on the hour
  @Transactional
  @Scheduled(cron = "0 0 * * * *")
  public void purge() {
    Instant now = Instant.now();
    // these repository methods must already exist as used in your tests
    repo.deleteAllExpired(now);
    repo.deleteAllRevokedOlderThan(now.minus(30, ChronoUnit.DAYS));
  }
}

