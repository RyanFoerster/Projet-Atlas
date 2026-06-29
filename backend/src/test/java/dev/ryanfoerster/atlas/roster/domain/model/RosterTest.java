package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.roster.domain.model.exceptions.MirrorAlreadyExistsException;
import dev.ryanfoerster.atlas.roster.domain.service.AthleteGenerator;
import dev.ryanfoerster.atlas.roster.domain.service.ProceduralAthleteGenerator;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class RosterTest {

    private static final Instant NOW = Instant.parse("2026-06-23T10:00:00Z");
    private final AthleteGenerator generator = new ProceduralAthleteGenerator();
    private final UserId owner = UserId.generate();

    private static MirrorCreationRequest mirrorRequest() {
        return new MirrorCreationRequest(AthleteName.of("Ryan"), 30, Weight.ofKilograms(80),
                Height.ofCentimeters(178), Gender.MALE,
                Map.of(MovementPattern.SQUAT, OneRepMax.measured(Weight.ofKilograms(140)),
                        MovementPattern.BENCH_PRESS, OneRepMax.measured(Weight.ofKilograms(100)),
                        MovementPattern.DEADLIFT, OneRepMax.measured(Weight.ofKilograms(180)),
                        MovementPattern.OVERHEAD_PRESS, OneRepMax.measured(Weight.ofKilograms(60))));
    }

    @Test
    void created_roster_is_empty() {
        Roster roster = Roster.createFor(owner, NOW);

        assertThat(roster.size()).isZero();
        assertThat(roster.hasMirror()).isFalse();
        assertThat(roster.ownerId()).isEqualTo(owner);
    }

    @Test
    void add_mirror_creates_the_one_mirror_and_leaves_the_original_untouched() {
        Roster empty = Roster.createFor(owner, NOW);

        Roster withMirror = empty.addMirror(mirrorRequest(), generator, 42L, NOW);

        assertThat(withMirror.hasMirror()).isTrue();
        assertThat(withMirror.size()).isEqualTo(1);
        assertThat(withMirror.mirrorAthlete()).get().satisfies(a -> {
            assertThat(a.isMirror()).isTrue();
            assertThat(a.name()).isEqualTo(AthleteName.of("Ryan"));
        });
        assertThat(empty.size()).isZero(); // immutabilité : l'original n'a pas bougé
    }

    @Test
    void add_mirror_twice_is_rejected() {
        Roster withMirror = Roster.createFor(owner, NOW).addMirror(mirrorRequest(), generator, 42L, NOW);

        assertThatExceptionOfType(MirrorAlreadyExistsException.class)
                .isThrownBy(() -> withMirror.addMirror(mirrorRequest(), generator, 7L, NOW));
    }

    @Test
    void recruit_adds_a_virtual_athlete() {
        Roster roster = Roster.createFor(owner, NOW).addMirror(mirrorRequest(), generator, 42L, NOW);
        AthleteCandidate candidate = generator.generateCandidate(99L, Rarity.SPECIALIST);

        Roster recruited = roster.recruit(candidate, NOW);

        assertThat(recruited.size()).isEqualTo(2);
        assertThat(recruited.virtualAthletes()).hasSize(1);
        assertThat(recruited.virtualAthletes().getFirst().isMirror()).isFalse();
        assertThat(recruited.hasMirror()).isTrue(); // le miroir est toujours là
    }

    @Test
    void equality_is_by_identity() {
        Roster roster = Roster.createFor(owner, NOW);
        Roster afterMirror = roster.addMirror(mirrorRequest(), generator, 42L, NOW);

        assertThat(afterMirror).isEqualTo(roster); // même RosterId, état différent
    }

    @Test
    void recording_a_workout_updates_only_the_mirror_training_history() {
        Roster roster = Roster.createFor(owner, NOW)
                .addMirror(mirrorRequest(), generator, 42L, NOW)
                .recruit(generator.generateCandidate(99L, Rarity.SPECIALIST), NOW);
        Instant performedAt = NOW.minusSeconds(3600);

        Roster updated = roster.recordMirrorWorkout(performedAt, Set.of(MovementPattern.SQUAT));

        assertThat(updated.mirrorAthlete()).get().satisfies(m -> {
            assertThat(m.trainingHistory().lastWorkoutAt()).isEqualTo(performedAt);
            assertThat(m.trainingHistory().lastPatternsCovered()).containsExactly(MovementPattern.SQUAT);
        });
        // L'athlète virtuel n'a pas d'historique d'entraînement (son training viendra de Programming).
        assertThat(updated.virtualAthletes().getFirst().trainingHistory().hasWorkouts()).isFalse();
    }

    @Test
    void recording_a_workout_without_a_mirror_is_a_no_op() {
        Roster empty = Roster.createFor(owner, NOW);

        Roster updated = empty.recordMirrorWorkout(NOW, Set.of(MovementPattern.SQUAT));

        assertThat(updated.size()).isZero();
    }

    @Test
    void progressing_a_stat_targets_only_the_named_athlete() {
        Roster roster = Roster.createFor(owner, NOW)
                .addMirror(mirrorRequest(), generator, 42L, NOW)
                .recruit(generator.generateCandidate(99L, Rarity.SPECIALIST), NOW);
        var mirrorId = roster.mirrorAthlete().orElseThrow().id();
        var virtualBefore = roster.virtualAthletes().getFirst();

        Roster updated = roster.progressAthleteStat(mirrorId, MovementPattern.SQUAT,
                OneRepMax.measured(Weight.ofKilograms(150)));

        assertThat(updated.findAthlete(mirrorId).orElseThrow()
                .currentOneRepMax(MovementPattern.SQUAT).orElseThrow().weight().toKilograms().doubleValue())
                .isEqualTo(150.0);
        // L'athlète virtuel n'a pas bougé.
        assertThat(updated.virtualAthletes().getFirst().currentStats())
                .isEqualTo(virtualBefore.currentStats());
    }
}
