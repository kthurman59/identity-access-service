package com.kevdev.iam.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
public class RefreshToken {
  @Id
  private UUID id;

  @Column(nullable = false, length = 100)
  private String subject;

  @Column(name = "token_hash", nullable = false, columnDefinition = "text")
  private String tokenHash;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "replaced_by")
  private UUID replacedBy;

  public static RefreshToken of(String subject, String tokenHash, Instant issued, Instant expires) {
    RefreshToken rt = new RefreshToken();
    rt.id = UUID.randomUUID();
    rt.subject = subject;
    rt.tokenHash = tokenHash;
    rt.issuedAt = issued;
    rt.expiresAt = expires;
    return rt;
  }

  public UUID getId() { return id; }
  public String getSubject() { return subject; }
  public String getTokenHash() { return tokenHash; }
  public Instant getIssuedAt() { return issuedAt; }
  public Instant getExpiresAt() { return expiresAt; }
  public Instant getRevokedAt() { return revokedAt; }
  public UUID getReplacedBy() { return replacedBy; }

  public void revokeNow() { this.revokedAt = Instant.now(); }
  public void markReplacedBy(UUID id) { this.replacedBy = id; }
}
