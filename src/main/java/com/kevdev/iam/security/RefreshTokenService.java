package com.kevdev.iam.security;

import com.kevdev.iam.domain.RefreshToken;
import com.kevdev.iam.repo.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenService {

  public static record TokenPair(String refreshToken, String username) {}

  private final RefreshTokenRepository repo;

  public RefreshTokenService(RefreshTokenRepository repo) {
    this.repo = repo;
  }

  // returns the opaque refresh token string
  @Transactional
  public String mintOnLogin(UUID userId, String username) {
    String raw = generateOpaque();
    String hash = sha256Hex(raw);

    RefreshToken rt = new RefreshToken();
    rt.setId(UUID.randomUUID());
    rt.setSubject(username);
    rt.setUsername(username);
    rt.setTokenHash(hash);
    Instant now = Instant.now();
    rt.setCreatedAt(now);
    rt.setUpdatedAt(now);
    rt.setExpiresAt(now.plus(Duration.ofDays(30)));

    repo.save(rt);
    // light cleanup
    repo.deleteExpiredOrRevoked(Instant.now());
    return raw;
  }

  // rotates if presented token is valid, returns new refresh token and the username
  @Transactional
  public TokenPair rotate(UUID userId, String presentedRefresh) {
    String hash = sha256Hex(Objects.requireNonNullElse(presentedRefresh, ""));
    Optional<RefreshToken> found = repo.findByTokenHash(hash);
    if (found.isEmpty()) throw new IllegalArgumentException("invalid refresh token");

    RefreshToken current = found.get();
    if (current.isExpired() || current.isRevoked()) throw new IllegalStateException("refresh not usable");

    current.setRevokedAt(Instant.now());
    repo.save(current);

    String nextRaw = generateOpaque();
    String nextHash = sha256Hex(nextRaw);

    RefreshToken next = new RefreshToken();
    next.setId(UUID.randomUUID());
    next.setSubject(current.getUsername());
    next.setUsername(current.getUsername());
    Instant now = Instant.now();
    next.setCreatedAt(now);
    next.setUpdatedAt(now);
    next.setExpiresAt(now.plus(Duration.ofDays(30)));
    next.setTokenHash(nextHash);
    repo.save(next);

    repo.deleteExpiredOrRevoked(Instant.now());
    return new TokenPair(nextRaw, current.getUsername());
  }

  private static String generateOpaque() {
    byte[] rnd = new byte[32];
    new java.security.SecureRandom().nextBytes(rnd);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
  }

  private static String sha256Hex(String s) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(out.length * 2);
      for (byte b : out) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}

