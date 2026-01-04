package com.kevdev.iam.repo;

import com.kevdev.iam.domain.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query("delete from RefreshToken rt where rt.expiresAt < :now or rt.revokedAt is not null")
  int deleteExpiredOrRevoked(@Param("now") Instant now);
}

