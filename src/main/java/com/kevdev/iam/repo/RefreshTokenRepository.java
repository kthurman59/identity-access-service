package com.kevdev.iam.repo;

import com.kevdev.iam.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select rt from RefreshToken rt where rt.tokenHash = :hash")
  Optional<RefreshToken> findForUpdateByTokenHash(@Param("hash") String hash);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Transactional
  @Query("delete from RefreshToken rt where rt.expiresAt < :now or rt.revokedAt is not null")
  int deleteExpiredOrRevoked(@Param("now") Instant now);
}

