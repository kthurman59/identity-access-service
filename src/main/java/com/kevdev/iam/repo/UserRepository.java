package com.kevdev.iam.repo;

import com.kevdev.iam.domain.IamUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<IamUser, UUID> {
    Optional<IamUser> findByTenantIdAndUsername(UUID tenantId, String username);
    Optional<IamUser> findByTenantIdAndEmail(UUID tenantId, String email);
}

