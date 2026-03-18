package com.vaultcore.config;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@Testcontainers
@SuppressWarnings("resource")
public abstract class IntegrationTestBase {

    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("vaultcore")
            .withUsername("vaultuser")
            .withPassword("vaultpass");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", IntegrationTestBase::jdbcUrl);
        registry.add("spring.datasource.username", IntegrationTestBase::dbUser);
        registry.add("spring.datasource.password", IntegrationTestBase::dbPass);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.cache.type", () -> "none");
        registry.add("spring.autoconfigure.exclude", () ->
            "org.redisson.spring.starter.RedissonAutoConfigurationV2"
        );
    }

    private static synchronized void ensureRunning() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
    }

    private static String jdbcUrl() {
        ensureRunning();
        return POSTGRES.getJdbcUrl();
    }

    private static String dbUser() {
        ensureRunning();
        return POSTGRES.getUsername();
    }

    private static String dbPass() {
        ensureRunning();
        return POSTGRES.getPassword();
    }
}