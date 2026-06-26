package dev.ryanfoerster.atlas.athletics.infrastructure.persistence;

import dev.ryanfoerster.atlas.AbstractIntegrationTest;
import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.ConditionSnapshot;
import dev.ryanfoerster.atlas.athletics.domain.model.FitnessFatigueState;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.athletics.domain.port.AthleteConditionRepository;
import dev.ryanfoerster.atlas.athletics.domain.port.ConditionSnapshotRepository;
import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
import dev.ryanfoerster.atlas.identity.domain.model.DisplayName;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteName;
import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.roster.domain.model.Height;
import dev.ryanfoerster.atlas.roster.domain.model.MirrorCreationRequest;
import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.roster.domain.service.AthleteGenerator;
import dev.ryanfoerster.atlas.roster.domain.service.ProceduralAthleteGenerator;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Round-trip de l'état dynamique + append/ordre des snapshots (Testcontainers, FK réelle vers athletes). */
class AthleteConditionPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-06-24T12:00:00Z");
    private static final Instant T0 = Instant.parse("2026-06-23T18:00:00Z");
    private static final AthleteGenerator GENERATOR = new ProceduralAthleteGenerator();

    @Autowired
    private AthleteConditionRepository conditionRepository;
    @Autowired
    private ConditionSnapshotRepository snapshotRepository;
    @Autowired
    private RosterRepository rosterRepository;
    @Autowired
    private UserRepository userRepository;

    @Test
    void persists_and_reloads_a_condition_exactly() {
        AthleteId athleteId = createMirrorAthlete();
        AthleteCondition condition = AthleteCondition.initial(athleteId, T0)
                .applyStimulus(new BanisterModel(), new TrainingStimulus(7.5), T0);

        conditionRepository.save(condition);
        AthleteCondition reloaded = conditionRepository.findByAthleteId(athleteId).orElseThrow();

        assertThat(reloaded).isEqualTo(condition); // égalité par identité
        assertThat(reloaded.state().fitness()).isEqualTo(7.5);
        assertThat(reloaded.state().fatigue()).isEqualTo(7.5);
        assertThat(reloaded.state().lastUpdated()).isEqualTo(T0);
    }

    @Test
    void snapshots_are_appended_and_read_back_in_chronological_order() {
        AthleteId athleteId = createMirrorAthlete();
        // Sauvés dans le désordre ; doivent ressortir triés par takenAt ASC (ordre des courbes).
        snapshotRepository.save(ConditionSnapshot.capture(athleteId,
                new FitnessFatigueState(5.0, 1.0, T0.plus(Duration.ofDays(2))), 3.0));
        snapshotRepository.save(ConditionSnapshot.capture(athleteId,
                new FitnessFatigueState(2.0, 2.0, T0), -2.0));
        snapshotRepository.save(ConditionSnapshot.capture(athleteId,
                new FitnessFatigueState(4.0, 1.5, T0.plus(Duration.ofDays(1))), 1.0));

        List<ConditionSnapshot> ordered = snapshotRepository.findByAthleteId(athleteId);

        assertThat(ordered).extracting(ConditionSnapshot::takenAt).containsExactly(
                T0, T0.plus(Duration.ofDays(1)), T0.plus(Duration.ofDays(2)));
        assertThat(ordered.getFirst().performance()).isEqualTo(-2.0); // négatif conservé
    }

    private AthleteId createMirrorAthlete() {
        User user = userRepository.save(User.register(
                Email.of("lifter-" + UUID.randomUUID() + "@example.com"), DisplayName.of("Lifter"),
                Locale.FRENCH, ZoneId.of("Europe/Brussels"), NOW));
        MirrorCreationRequest request = new MirrorCreationRequest(AthleteName.of("Ryan"), 30,
                Weight.ofKilograms(80), Height.ofCentimeters(178), Gender.MALE,
                Map.of(MovementPattern.SQUAT, OneRepMax.measured(Weight.ofKilograms(140)),
                        MovementPattern.BENCH_PRESS, OneRepMax.measured(Weight.ofKilograms(100)),
                        MovementPattern.DEADLIFT, OneRepMax.measured(Weight.ofKilograms(180)),
                        MovementPattern.OVERHEAD_PRESS, OneRepMax.measured(Weight.ofKilograms(60))));
        rosterRepository.save(Roster.createFor(user.id(), NOW).addMirror(request, GENERATOR, 42L, NOW));
        return rosterRepository.findByOwnerId(user.id()).orElseThrow().mirrorAthlete().orElseThrow().id();
    }
}
