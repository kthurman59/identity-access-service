package com.kevdev.iam.service;

import com.kevdev.iam.domain.RefreshToken;
import com.kevdev.iam.repo.RefreshTokenRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Service
public class RefreshTokenService {

  private final RefreshTokenRepository repo;

  public RefreshTokenService(RefreshTokenRepository repo) {
    this.repo = repo;
  }

  public String issue(String subject, long ttlSeconds) {
    String raw = RandomTokens.base64Url(48);
    String hash = hash(raw);
    Instant now = Instant.now();
    RefreshToken rt = RefreshToken.of(subject, hash, now, now.plusSeconds(ttlSeconds));
    repo.save(rt);
    return raw;
  }

  public record RotateResult(String subject, String newRefreshToken) {}

  public RotateResult rotate(String rawToken, long ttlSeconds) {
    String hash = hash(rawToken);
    RefreshToken existing = repo.findFirstByTokenHashAndRevokedAtIsNull(hash)
        .orElseThrow(() -> new IllegalArgumentException("invalid refresh token"));

    if (existing.getExpiresAt().isBefore(Instant.now())) {
      throw new IllegalArgumentException("expired refresh token");
  }

  existing.revokeNow();

  String nextRaw = RandomTokens.base64Url(48);
  String nextHash = hash(nextRaw);
  Instant now = Instant.now();
  RefreshToken next = RefreshToken.of(existing.getSubject(), nextHash, now, now.plusSeconds(ttlSeconds));
  existing.markReplacedBy(next.getId());

  repo.save(existing);
  repo.save(next);

  return new RotateResult(existing.getSubject(), nextRaw);
}

  private static String hash(String raw) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] d = md.digest(raw.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder().encodeToString(d);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}

