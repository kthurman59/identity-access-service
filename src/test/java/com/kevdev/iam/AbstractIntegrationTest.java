package com.kevdev.iam;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Testcontainers
@ActiveProfiles("test")
@Import(AbstractIntegrationTest.FlywayTestConfig.class)
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("iam_test")
            .withUsername("iam_test")
            .withPassword("iam_test");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);

        r.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        r.add("spring.flyway.user", POSTGRES::getUsername);
        r.add("spring.flyway.password", POSTGRES::getPassword);

        r.add("spring.flyway.clean-disabled", () -> "false");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @TestConfiguration
    static class FlywayTestConfig {

        @Bean
        FlywayMigrationStrategy flywayMigrationStrategy(DataSource dataSource) {
            return flyway -> {
                resetSchema(dataSource);
                flyway.migrate();
            };
        }

        private static void resetSchema(DataSource dataSource) {
            try (Connection c = dataSource.getConnection();
                 Statement s = c.createStatement()) {
                s.execute("drop schema if exists public cascade");
                s.execute("create schema public");
            } catch (Exception e) {
                throw new IllegalStateException("Failed to reset schema for tests", e);
            }
        }
    }
}

