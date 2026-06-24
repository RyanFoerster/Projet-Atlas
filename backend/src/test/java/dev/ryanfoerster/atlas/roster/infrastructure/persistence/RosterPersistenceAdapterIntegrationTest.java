package dev.ryanfoerster.atlas.roster.infrastructure.persistence;

import dev.ryanfoerster.atlas.AbstractIntegrationTest;
import dev.ryanfoerster.atlas.identity.domain.model.DisplayName;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import dev.ryanfoerster.atlas.identity.infrastructure.persistence.UserJpaRepository;
import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteName;
import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.roster.domain.model.Height;
import dev.ryanfoerster.atlas.roster.domain.model.MirrorCreationRequest;
import dev.ryanfoerster.atlas.roster.domain.model.Rarity;
import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.roster.domain.service.AthleteGenerator;
import dev.ryanfoerster.atlas.roster.domain.service.ProceduralAthleteGenerator;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RosterPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-06-23T10:00:00Z");
    private final AthleteGenerator generator = new ProceduralAthleteGenerator();

    @Autowired
    private RosterRepository rosterRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RosterJpaRepository rosterJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UserId createOwner() {
        User user = userRepository.save(User.register(
                Email.of("owner-" + UUID.randomUUID() + "@example.com"), DisplayName.of("Owner"),
                Locale.FRENCH, ZoneId.of("Europe/Brussels"), NOW));
        return user.id();
    }

    private static MirrorCreationRequest mirrorRequest() {
        return new MirrorCreationRequest(AthleteName.of("Ryan"), 30, Weight.ofKilograms(80),
                Height.ofCentimeters(178), Gender.MALE,
                Map.of(MovementPattern.SQUAT, OneRepMax.measured(Weight.ofKilograms(140)),
                        MovementPattern.BENCH_PRESS, OneRepMax.measured(Weight.ofKilograms(100)),
                        MovementPattern.DEADLIFT, OneRepMax.measured(Weight.ofKilograms(180)),
                        MovementPattern.OVERHEAD_PRESS, OneRepMax.measured(Weight.ofKilograms(60))));
    }

    private Roster rosterWithThreeAthletes(UserId owner) {
        return Roster.createFor(owner, NOW)
                .addMirror(mirrorRequest(), generator, 42L, NOW)
                .recruit(generator.generateCandidate(1L, Rarity.SPECIALIST), NOW)
                .recruit(generator.generateCandidate(2L, Rarity.PRODIGY), NOW);
    }

    @Test
    void persists_and_reloads_the_whole_aggregate_with_its_athletes() {
        UserId owner = createOwner();
        Roster roster = rosterWithThreeAthletes(owner);

        rosterRepository.save(roster);
        Roster reloaded = rosterRepository.findById(roster.id()).orElseThrow();

        assertThat(reloaded).isEqualTo(roster);          // égalité par identité (RosterId)
        assertThat(reloaded.size()).isEqualTo(3);
        assertThat(reloaded.hasMirror()).isTrue();
        assertThat(reloaded.virtualAthletes()).hasSize(2);
        assertThat(reloaded.ownerId()).isEqualTo(owner);
    }

    @Test
    void genetics_and_stats_round_trip_through_jsonb_exactly() {
        UserId owner = createOwner();
        Roster roster = rosterWithThreeAthletes(owner);
        rosterRepository.save(roster);

        Athlete original = roster.mirrorAthlete().orElseThrow();
        Athlete reloaded = rosterRepository.findById(roster.id()).orElseThrow().mirrorAthlete().orElseThrow();

        // C1 : round-trip Genetics complexe → JSONB → Genetics, égalité PAR VALEUR
        assertThat(reloaded.genetics()).isEqualTo(original.genetics());
        assertThat(reloaded.currentStats()).isEqualTo(original.currentStats());
        assertThat(reloaded.bodyWeight().toKilograms()).isEqualByComparingTo(original.bodyWeight().toKilograms());
    }

    @Test
    void genetics_column_is_real_native_jsonb() {
        UserId owner = createOwner();
        Roster roster = Roster.createFor(owner, NOW).addMirror(mirrorRequest(), generator, 42L, NOW);
        rosterRepository.save(roster);
        UUID mirrorId = roster.mirrorAthlete().orElseThrow().id().value();

        // C3 : prouve que c'est du vrai jsonb natif (pas du text/bytea) — sinon jsonb_typeof échoue.
        String type = jdbcTemplate.queryForObject(
                "SELECT jsonb_typeof(genetics) FROM athletes WHERE id = ?", String.class, mirrorId);

        assertThat(type).isEqualTo("object");
    }

    @Test
    void finds_a_roster_by_its_owner() {
        UserId owner = createOwner();
        Roster roster = Roster.createFor(owner, NOW).addMirror(mirrorRequest(), generator, 42L, NOW);
        rosterRepository.save(roster);

        assertThat(rosterRepository.findByOwnerId(owner)).get().isEqualTo(roster);
    }
}
