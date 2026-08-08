package com.respondr;

import org.junit.jupiter.api.Test;

/**
 * Smoke test — verifies the Spring application context loads without errors.
 * Uses a PostgreSQL Testcontainer supplied by {@link AbstractIntegrationTest}.
 */
class RespondrApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // If the application context fails to start, this test fails automatically.
    }
}
