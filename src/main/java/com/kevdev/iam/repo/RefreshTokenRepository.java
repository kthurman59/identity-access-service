package com.kevdev.iam.repo;

import com.kevdev.iam.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByTenantKeyAndTokenHash(String tenantKey, String tokenHash);

  @Modifying
  @Query("delete from RefreshToken r where r.expiresAt < :now")
  int deleteAllExpired(@Param("now") Instant now);

  @Modifying
  @Query("delete from RefreshToken r where r.revokedAt is not null and r.updatedAt < :cutoff")
  int deleteAllRevokedOlderThan(@Param("cutoff") Instant cutoff);
}

