package dev.ryanfoerster.atlas;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base des tests d'intégration : démarre un vrai PostgreSQL via Testcontainers (jamais H2, ADR-008).
 *
 * <p><b>Singleton container pattern</b> : le container est un champ {@code static} démarré une
 * seule fois et partagé par toutes les classes de test qui héritent de cette base. Il n'est jamais
 * arrêté explicitement — Testcontainers (via Ryuk) le nettoie à la fin de la JVM. On évite ainsi
 * de payer le coût de démarrage d'un container par classe de test. L'isolation des données entre
 * tests se fait par truncate des tables, pas par recreate du container.
 *
 * <p><b>{@code @ServiceConnection}</b> (Spring Boot 3.1+) : Spring Boot détecte ce champ container
 * et configure automatiquement la {@code DataSource} (url/user/password/driver) à partir de lui.
 * Plus besoin du {@code @DynamicPropertySource} manuel des versions précédentes.
 *
 * <p>La version de l'image PostgreSQL provient de la propriété Maven {@code postgres.image.version}
 * (source de vérité unique, cf. pom.xml et docker-compose.yml), passée aux tests par surefire.
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
}
