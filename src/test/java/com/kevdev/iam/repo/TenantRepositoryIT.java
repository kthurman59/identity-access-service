package com.kevdev.iam.repo;

import com.kevdev.iam.AbstractIntegrationTest;
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
class TenantRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    TenantRepository tenantRepository;

    @Test
    void savesTenant() {
        long before = tenantRepository.count();

        Tenant t = Tenant.create("acme", "Acme");
        tenantRepository.saveAndFlush(t);

        assertThat(tenantRepository.count()).isEqualTo(before + 1);
    }

    @Test
    void enforcesUniqueSlug() {
        Tenant t1 = Tenant.create("acme", "Acme One");
        tenantRepository.saveAndFlush(t1);

        Tenant t2 = Tenant.create("acme", "Acme Two");

        assertThatThrownBy(() -> tenantRepository.saveAndFlush(t2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}

