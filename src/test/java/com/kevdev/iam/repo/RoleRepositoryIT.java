package com.kevdev.iam.repo;

import com.kevdev.iam.AbstractIntegrationTest;
import com.kevdev.iam.domain.Role;
import com.kevdev.iam.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;

class RoleRepositoryIT extends AbstractIntegrationTest {

    @Autowired TenantRepository tenants;
    @Autowired RoleRepository roles;

    @Test
    void roleNameUniqueWithinTenant() {
        Tenant t = new Tenant();
        t.setSlug("t1");
        t.setName("Tenant 1");
        t = tenants.saveAndFlush(t);

        Role r1 = new Role();
        r1.setTenant(t);
        r1.setName("ADMIN");
        roles.saveAndFlush(r1);

        Role r2 = new Role();
        r2.setTenant(t);
        r2.setName("ADMIN");

        assertThrows(DataIntegrityViolationException.class, () -> roles.saveAndFlush(r2));
    }
}

