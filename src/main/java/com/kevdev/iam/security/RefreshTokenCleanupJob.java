package com.kevdev.iam.security;

import com.kevdev.iam.repo.RefreshTokenRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class RefreshTokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;
    private final Clock clock = Clock.systemUTC();

    public RefreshTokenCleanupJob(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Scheduled(fixedDelay = 3600000)
    public void deleteExpired() {
        Instant now = Instant.now(clock);
        refreshTokenRepository.deleteByExpiresAtBefore(now);
    }
}

