package com.kevdev.iam.repo;

import com.kevdev.iam.AbstractIntegrationTest;
import com.kevdev.iam.domain.Role;
import com.kevdev.iam.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class RoleRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    RoleRepository roleRepository;

    @Test
    void savesRolesForTenant() {
        Tenant t = Tenant.create("acme", "Acme");
        tenantRepository.saveAndFlush(t);

        Role r1 = Role.create(t, "ADMIN");
        Role r2 = Role.create(t, "USER");

        roleRepository.saveAndFlush(r1);
        roleRepository.saveAndFlush(r2);

        var roles = roleRepository.findAll();
        assertThat(roles).hasSizeGreaterThanOrEqualTo(2);
    }
}

