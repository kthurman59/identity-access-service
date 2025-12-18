package com.kevdev.iam.repo;

import com.kevdev.iam.AbstractIntegrationTest;
import com.kevdev.iam.domain.IamUser;
import com.kevdev.iam.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class UserRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    UserRepository userRepository;

    @Test
    void savesUsers() {
        Tenant t = Tenant.create("acme", "Acme");
        tenantRepository.saveAndFlush(t);

        long before = userRepository.count();

        IamUser u = IamUser.create(t, "kevin", "kevin@example.com", "hash");
        userRepository.saveAndFlush(u);

        assertThat(userRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void enforcesUniqueUsernamePerTenant() {
        Tenant t = Tenant.create("acme", "Acme");
        tenantRepository.saveAndFlush(t);

        IamUser u1 = IamUser.create(t, "kevin", "kevin1@example.com", "hash1");
        userRepository.saveAndFlush(u1);

        IamUser u2 = IamUser.create(t, "kevin", "kevin2@example.com", "hash2");

        assertThatThrownBy(() -> userRepository.saveAndFlush(u2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

