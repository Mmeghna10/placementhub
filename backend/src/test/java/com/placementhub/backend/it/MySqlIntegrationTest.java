package com.placementhub.backend.it;

import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for tests that need a real MySQL database (Flyway migrations, transactions,
 * query counts). Mocks cannot show any of those.
 *
 * Configure with environment variables; the tests are skipped when they are absent:
 *   PLACEMENTHUB_IT_DB_URL       e.g. jdbc:mysql://localhost:3306/placementhub_it
 *   PLACEMENTHUB_IT_DB_USER
 *   PLACEMENTHUB_IT_DB_PASSWORD
 *
 * The database is created by Flyway from scratch, so it must be dedicated to tests: the
 * name has to end in "_it" or "_test", which keeps these tests away from dev or prod data.
 */
@SpringBootTest(properties = {
        "app.jwt.secret=integration-test-secret-of-at-least-32-bytes",
        "app.jwt.expiration-ms=3600000",
        "gemini.api.key=unused-in-tests",
        "gemini.api.model=unused-in-tests",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        // Concurrency tests run many transactions at the same moment; with the normal pool
        // of 10 they would queue for connections and never really race.
        "spring.datasource.hikari.maximum-pool-size=40",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=WARN"
})
@EnabledIfEnvironmentVariable(named = MySqlIntegrationTest.URL_ENV, matches = ".+")
public abstract class MySqlIntegrationTest {

    static final String URL_ENV = "PLACEMENTHUB_IT_DB_URL";

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        String url = System.getenv(URL_ENV);
        String database = url.replaceFirst("\\?.*$", "").replaceFirst("^.*/", "");
        if (!database.endsWith("_it") && !database.endsWith("_test")) {
            throw new IllegalStateException(URL_ENV + " must point at a dedicated test database "
                    + "whose name ends in _it or _test, but it points at '" + database + "'");
        }
        registry.add("spring.datasource.url", () -> url);
        registry.add("spring.datasource.username", () -> System.getenv("PLACEMENTHUB_IT_DB_USER"));
        registry.add("spring.datasource.password", () -> System.getenv("PLACEMENTHUB_IT_DB_PASSWORD"));
    }
}
