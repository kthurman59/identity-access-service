package com.kevdev.iam;

import com.kevdev.iam.domain.IamUser;
import com.kevdev.iam.domain.Permission;
import com.kevdev.iam.domain.Role;
import com.kevdev.iam.domain.Tenant;
import com.kevdev.iam.repo.IamUserRepository;
import com.kevdev.iam.repo.PermissionRepository;
import com.kevdev.iam.repo.RoleRepository;
import com.kevdev.iam.repo.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

class RbacPersistenceTest extends AbstractIntegrationTest {

    @Autowired
    TenantRepository tenants;

    @Autowired
    RoleRepository roles;

    @Autowired
    PermissionRepository permissions;

    @Autowired
    IamUserRepository users;

    @Test
    @Transactional
    void canCreateTenantUserRolePermissionAndAssign() {
        Tenant tenant = tenants.save(Tenant.create("acme", "Acme Inc"));

        Permission permission = permissions.save(new Permission("USER_READ", "Read users"));

        Role role = Role.create(tenant, "ADMIN", "Admin role");
        role.getPermissions().add(permission);
        role = roles.save(role);

        IamUser user = IamUser.create(tenant, "kevin", "kevin@example.com", "hash");
        user.assignRole(role);
        user = users.save(user);

        IamUser loaded = users.findById(user.getId()).orElseThrow();
        assertThat(loaded.getTenant().getId()).isEqualTo(tenant.getId());
        assertThat(loaded.getRoles()).hasSize(1);
    }
}

