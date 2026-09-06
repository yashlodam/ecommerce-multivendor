package com.zosh;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Smoke test — verifies the Spring application context loads.
 *
 * Requires a running PostgreSQL database (or configure an in-memory H2 for CI).
 * Run with: mvn test -Pintegration
 *
 * To run unit tests only (no DB required): mvn test -Dtest="!EcommerceMultivendorApplicationTests"
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce_multivendor",
    "spring.jpa.hibernate.ddl-auto=none",
    "app.jwt.secret=test-secret-key-at-least-64-characters-long-for-hs512-algorithm",
    "payment.razorpay.key-id=rzp_test_key",
    "payment.razorpay.key-secret=test_secret",
    "app.email.brevo.api-key=test-brevo-api-key",
    "app.email.brevo.from-email=noreply@shopsphere.com",
    "app.email.brevo.from-name=ShopSphere",
    "app.admin.email=admin@test.com",
    "app.admin.password=Admin@Test123"
})
class EcommerceMultivendorApplicationTests {

    @Test
    void contextLoads() {
        // Verifies that all Spring beans load without errors.
        // This test requires a running PostgreSQL instance.
        // For CI without DB: use @DataJpaTest or H2 in-memory.
    }
}
