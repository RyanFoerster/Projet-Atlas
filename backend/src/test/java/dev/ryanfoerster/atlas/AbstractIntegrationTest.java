package dev.ryanfoerster.atlas;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

/**
 * Base des tests d'intégration : démarre un vrai PostgreSQL via Testcontainers (jamais H2, ADR-008).
 *
 * <p><b>Singleton container pattern</b> : le container est un champ {@code static} démarré une
 * seule fois et partagé par toutes les classes de test qui héritent de cette base. Il n'est jamais
 * arrêté explicitement — Testcontainers (via Ryuk) le nettoie à la fin de la JVM. On évite ainsi
 * de payer le coût de démarrage d'un container par classe de test.
 *
 * <p><b>Nettoyage centralisé (source de vérité unique)</b> : l'état est partagé entre <em>toutes</em>
 * les classes de test (un seul container, un seul JVM). Chaque test démarre donc d'une base
 * <strong>vierge</strong> grâce au {@link #resetDatabase()} en {@code @BeforeEach} ici — pas dans chaque
 * classe. On utilise {@code TRUNCATE … CASCADE} : PostgreSQL gère lui-même l'ordre des FK (enfants avant
 * parents), donc aucun test n'a à connaître cet ordre. Les tables sont <strong>découvertes
 * dynamiquement</strong> via {@code pg_tables} → toute table d'un futur sprint est nettoyée
 * automatiquement, sans maintenir de liste. La table {@code event_publication} (registry Modulith) est
 * incluse à dessein : on truncate <em>entre</em> les tests (jamais pendant un flow — les tests
 * event-driven attendent la consommation async avant de finir), ce qui évite qu'une publication
 * incomplète laissée par un test ne soit rejouée dans un test suivant. Seul {@code flyway_schema_history}
 * est exclu (journal des migrations, ne jamais truncate).
 *
 * <p><b>{@code @BeforeEach} (pas {@code @AfterEach})</b> : la base est propre avant chaque test, quoi
 * qu'il se soit passé avant (même si un test précédent a planté avant son nettoyage). JUnit 5 exécute le
 * {@code @BeforeEach} de cette classe parente <em>avant</em> celui des sous-classes (parent-first), donc le
 * truncate passe avant tout setup spécifique.
 *
 * <p><b>Limite assumée : suppose {@code forkCount=1}</b> (exécution séquentielle des classes, notre cas —
 * cf. config Surefire). Un {@code TRUNCATE} sur une base partagée est incompatible avec une exécution
 * parallèle. Si l'on parallélise un jour les tests d'intégration, prévoir un schéma (ou une base) par fork.
 *
 * <p><b>{@code @ServiceConnection}</b> (Spring Boot 3.1+) : Spring Boot détecte ce champ container et
 * configure automatiquement la {@code DataSource}. La version de l'image PostgreSQL provient de la propriété
 * Maven {@code postgres.image.version} (source de vérité unique, cf. pom.xml et docker-compose.yml).
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final String POSTGRES_VERSION = System.getProperty("postgres.image.version", "17");

    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:" + POSTGRES_VERSION));

    static {
        POSTGRES.start();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Base vierge avant chaque test : TRUNCATE CASCADE de toutes les tables métier (sauf le journal Flyway). */
    @BeforeEach
    void resetDatabase() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' "
                        + "AND tablename <> 'flyway_schema_history'",
                String.class);
        if (!tables.isEmpty()) {
            jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
        }
    }
}
