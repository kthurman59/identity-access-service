package com.kevdev.iam.repo;

import com.kevdev.iam.AbstractIntegrationTest;
import com.kevdev.iam.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;

class TenantRepositoryIT extends AbstractIntegrationTest {

    @Autowired TenantRepository tenants;

    @Test
    void uniqueSlugIsEnforced() {
        Tenant t1 = new Tenant();
        t1.setSlug("acme");
        t1.setName("Acme");
        tenants.saveAndFlush(t1);

        Tenant t2 = new Tenant();
        t2.setSlug("acme");
        t2.setName("Acme 2");

        assertThrows(DataIntegrityViolationException.class, () -> tenants.saveAndFlush(t2));
    }
}

