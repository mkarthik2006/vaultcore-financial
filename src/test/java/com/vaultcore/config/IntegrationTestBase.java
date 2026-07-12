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
        // No Redis in the test context: disable the Redisson-backed Hibernate L2 cache (Account is
        // @Cache-annotated) and the rate limiter so tests neither require Redis nor throttle.
        registry.add("spring.jpa.properties.hibernate.cache.use_second_level_cache", () -> "false");
        registry.add("spring.jpa.properties.hibernate.cache.use_query_cache", () -> "false");
        registry.add("app.rate-limit.enabled", () -> "false");
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