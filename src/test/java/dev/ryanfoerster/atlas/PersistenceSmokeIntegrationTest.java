package dev.ryanfoerster.atlas;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test du data layer (étape 3 du bootstrap) : prouve que le contexte Spring démarre
 * contre un vrai PostgreSQL et que Flyway a exécuté la migration baseline V001.
 */
class PersistenceSmokeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void flywayBaselineMigrationHasBeenApplied() {
        Integer successfulMigrations = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success = true",
                Integer.class);
        assertThat(successfulMigrations).isGreaterThanOrEqualTo(1);
    }

    @Test
    void runsAgainstRealPostgres() {
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
        assertThat(version).contains("PostgreSQL");
    }
}
