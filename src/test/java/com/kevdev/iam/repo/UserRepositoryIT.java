package com.kevdev.iam.repo;

import com.kevdev.iam.AbstractIntegrationTest;
import com.kevdev.iam.domain.IamUser;
import com.kevdev.iam.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.*;

class UserRepositoryIT extends AbstractIntegrationTest {

    @Autowired TenantRepository tenants;
    @Autowired UserRepository users;

    @Test
    void usernameUniqueWithinTenant() {
        Tenant t = new Tenant();
        t.setSlug("t1");
        t.setName("Tenant 1");
        t = tenants.saveAndFlush(t);

        IamUser u1 = new IamUser();
        u1.setTenant(t);
        u1.setUsername("kevin");
        u1.setEmail("kevin@t1.com");
        u1.setPasswordHash("x");
        users.saveAndFlush(u1);

        IamUser u2 = new IamUser();
        u2.setTenant(t);
        u2.setUsername("kevin");
        u2.setEmail("kevin2@t1.com");
        u2.setPasswordHash("x");

        assertThrows(DataIntegrityViolationException.class, () -> users.saveAndFlush(u2));
    }
}

