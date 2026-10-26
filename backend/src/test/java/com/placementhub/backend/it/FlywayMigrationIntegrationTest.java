package com.placementhub.backend.it;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The application context only starts if Flyway migrated the database AND Hibernate's
 * ddl-auto=validate accepted the result, so reaching these assertions already proves the
 * migrations produce a schema that matches the entities.
 */
class FlywayMigrationIntegrationTest extends MySqlIntegrationTest {

    @Autowired JdbcTemplate jdbc;

    @Test
    void allMigrationsAppliedSuccessfully() {
        List<String> versions = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success = 1 AND version IS NOT NULL ORDER BY installed_rank",
                String.class);

        assertThat(versions).contains("1", "2", "3", "4", "5");
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0", Integer.class)).isZero();
    }

    @Test
    void statusIndexesExist() {
        List<String> indexes = jdbc.queryForList("""
                SELECT DISTINCT index_name FROM information_schema.statistics
                WHERE table_schema = DATABASE() AND table_name IN ('jobs', 'applications')
                """, String.class);

        assertThat(indexes).contains("idx_jobs_status_posted_at", "idx_applications_status");
    }
}
