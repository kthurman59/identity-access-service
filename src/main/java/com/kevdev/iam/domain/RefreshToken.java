package com.kevdev.iam.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_token",
       indexes = {
         @Index(name = "ix_refresh_token_hash", columnList = "token_hash"),
         @Index(name = "ix_refresh_token_expires", columnList = "expires_at")
       })
public class RefreshToken {

  @Id
  @Column(name = "id", nullable = false, columnDefinition = "uuid")
  private UUID id;

  @Column(name = "subject", nullable = false, length = 200)
  private String subject;

  @Column(name = "username", nullable = false, length = 200)
  private String username;

  @Column(name = "token_hash", nullable = false, length = 128, unique = true)
  private String tokenHash;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  public RefreshToken() {}

  public UUID getId() { return id; }
  public void setId(UUID id) { this.id = id; }

  public String getSubject() { return subject; }
  public void setSubject(String subject) { this.subject = subject; }

  public String getUsername() { return username; }
  public void setUsername(String username) { this.username = username; }

  public String getTokenHash() { return tokenHash; }
  public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

  public Instant getRevokedAt() { return revokedAt; }
  public void setRevokedAt(Instant revokedAt) { this.revokedAt = revokedAt; }

  @PrePersist
  public void prePersist() {
    Instant now = Instant.now();
    if (id == null) id = UUID.randomUUID();
    if (createdAt == null) createdAt = now;
    if (updatedAt == null) updatedAt = now;
  }

  @PreUpdate
  public void preUpdate() {
    updatedAt = Instant.now();
  }

  @Transient
  public boolean isRevoked() { return revokedAt != null; }

  @Transient
  public boolean isExpired() { return expiresAt != null && expiresAt.isBefore(Instant.now()); }
}

