package com.kevdev.iam.repo;

import com.kevdev.iam.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByTenantIdAndName(UUID tenantId, String name);
}

