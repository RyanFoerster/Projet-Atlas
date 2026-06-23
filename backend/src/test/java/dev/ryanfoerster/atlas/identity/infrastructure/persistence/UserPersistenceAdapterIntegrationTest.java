package dev.ryanfoerster.atlas.identity.infrastructure.persistence;

import dev.ryanfoerster.atlas.AbstractIntegrationTest;
import dev.ryanfoerster.atlas.identity.domain.model.DisplayName;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests d'intégration du {@link UserPersistenceAdapter} sur un vrai PostgreSQL (Testcontainers,
 * ADR-008). On teste à travers le <em>port</em> {@link UserRepository} : le test ignore qu'il
 * y a JPA dessous, comme le domaine.
 */
class UserPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    // Instant à précision microseconde (PostgreSQL TIMESTAMPTZ ne garde pas les nanos) → round-trip exact.
    private static final Instant NOW = Instant.parse("2026-06-23T10:00:00.123456Z");
    private static final ZoneId TZ = ZoneId.of("Europe/Brussels");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserJpaRepository jpaRepository;

    @BeforeEach
    void cleanUp() {
        jpaRepository.deleteAll();
    }

    @Test
    void saves_and_finds_by_id_with_all_fields_round_tripping() {
        User user = User.register(Email.of("ryan@example.com"), DisplayName.of("Ryan"),
                Locale.FRENCH, TZ, NOW);

        userRepository.save(user);
        Optional<User> found = userRepository.findById(user.id());

        assertThat(found).isPresent();
        User reloaded = found.orElseThrow();
        assertThat(reloaded).isEqualTo(user);                 // égalité par identité
        assertThat(reloaded.email()).isEqualTo(user.email());
        assertThat(reloaded.displayName()).isEqualTo(user.displayName());
        assertThat(reloaded.locale()).isEqualTo(Locale.FRENCH);
        assertThat(reloaded.timezone()).isEqualTo(TZ);
        assertThat(reloaded.createdAt()).isEqualTo(NOW);
        assertThat(reloaded.lastLoginAt()).isEmpty();
    }

    @Test
    void finds_by_email() {
        User user = User.register(Email.of("ryan@example.com"), DisplayName.of("Ryan"),
                Locale.FRENCH, TZ, NOW);
        userRepository.save(user);

        assertThat(userRepository.findByEmail(Email.of("ryan@example.com"))).contains(user);
        assertThat(userRepository.findByEmail(Email.of("unknown@example.com"))).isEmpty();
    }

    @Test
    void exists_by_email() {
        userRepository.save(User.register(Email.of("ryan@example.com"), DisplayName.of("Ryan"),
                Locale.FRENCH, TZ, NOW));

        assertThat(userRepository.existsByEmail(Email.of("ryan@example.com"))).isTrue();
        assertThat(userRepository.existsByEmail(Email.of("nope@example.com"))).isFalse();
    }

    @Test
    void persists_an_updated_aggregate_via_save() {
        User user = User.register(Email.of("ryan@example.com"), DisplayName.of("Ryan"),
                Locale.FRENCH, TZ, NOW);
        userRepository.save(user);

        Instant loginTime = NOW.plusSeconds(3600);
        userRepository.save(user.recordLogin(loginTime));

        User reloaded = userRepository.findById(user.id()).orElseThrow();
        assertThat(reloaded.lastLoginAt()).contains(loginTime);
    }

    @Test
    void enforces_email_uniqueness_at_the_database_level() {
        userRepository.save(User.register(Email.of("ryan@example.com"), DisplayName.of("Ryan"),
                Locale.FRENCH, TZ, NOW));

        User duplicate = User.register(Email.of("ryan@example.com"), DisplayName.of("Other"),
                Locale.ENGLISH, TZ, NOW);

        assertThatExceptionOfType(DataIntegrityViolationException.class)
                .isThrownBy(() -> userRepository.save(duplicate));
    }
}
