package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.roster.domain.model.exceptions.InvalidAgeException;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;
import dev.ryanfoerster.atlas.shared.domain.Weight;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AthleteTest {

    private static final Instant NOW = Instant.parse("2026-06-23T10:00:00Z");

    private static Athlete reconstituted(AthleteId id, int age, Rarity rarity) {
        return Athlete.reconstitute(id, RosterId.generate(), AthleteName.of("Marcus Vélaris"), age,
                Weight.ofKilograms(80), Height.ofCentimeters(180), Gender.MALE, GeneticsTest.valid(),
                new CurrentStats(Map.of(MovementPattern.SQUAT, OneRepMax.measured(Weight.ofKilograms(140)))),
                rarity, false, NOW, TrainingHistory.empty());
    }

    @Test
    void reconstitute_builds_an_athlete_with_its_state() {
        AthleteId id = AthleteId.generate();
        Athlete a = reconstituted(id, 30, Rarity.SPECIALIST);

        assertThat(a.id()).isEqualTo(id);
        assertThat(a.age()).isEqualTo(30);
        assertThat(a.rarity()).isEqualTo(Rarity.SPECIALIST);
        assertThat(a.isMirror()).isFalse();
    }

    @Test
    void rejects_age_out_of_range() {
        assertThatExceptionOfType(InvalidAgeException.class).isThrownBy(() -> reconstituted(AthleteId.generate(), 15, Rarity.GENERIC));
        assertThatExceptionOfType(InvalidAgeException.class).isThrownBy(() -> reconstituted(AthleteId.generate(), 51, Rarity.GENERIC));
    }

    @Test
    void current_one_rep_max_reads_from_stats() {
        Athlete a = reconstituted(AthleteId.generate(), 30, Rarity.GENERIC);

        assertThat(a.currentOneRepMax(MovementPattern.SQUAT)).isPresent();
        assertThat(a.currentOneRepMax(MovementPattern.BENCH_PRESS)).isEmpty();
    }

    @Test
    void equality_is_by_identity_not_state() {
        AthleteId id = AthleteId.generate();
        Athlete a = reconstituted(id, 30, Rarity.GENERIC);
        Athlete sameIdOtherState = reconstituted(id, 40, Rarity.PRODIGY);

        assertThat(sameIdOtherState).isEqualTo(a);
        assertThat(reconstituted(AthleteId.generate(), 30, Rarity.GENERIC)).isNotEqualTo(a);
    }

    // --- Progression structurelle du 1RM (Couche 3, cliquet monotone) --------------------------

    @Test
    void progress_one_rep_max_raises_the_stat() {
        Athlete a = reconstituted(AthleteId.generate(), 30, Rarity.GENERIC); // squat 140

        Athlete progressed = a.progressOneRepMax(MovementPattern.SQUAT, OneRepMax.measured(Weight.ofKilograms(150)));

        assertThat(progressed.currentOneRepMax(MovementPattern.SQUAT).orElseThrow().weight().toKilograms().doubleValue())
                .isEqualTo(150.0);
    }

    @Test
    void progress_one_rep_max_is_a_no_op_below_or_equal_to_current_cliquet() {
        Athlete a = reconstituted(AthleteId.generate(), 30, Rarity.GENERIC); // squat 140

        // Sous le courant (un event périmé/réordonnancé) → no-op, MÊME instance (cliquet : jamais à la baisse).
        assertThat(a.progressOneRepMax(MovementPattern.SQUAT, OneRepMax.measured(Weight.ofKilograms(130)))).isSameAs(a);
        // Égal au courant (rejeu idempotent) → no-op.
        assertThat(a.progressOneRepMax(MovementPattern.SQUAT, OneRepMax.measured(Weight.ofKilograms(140)))).isSameAs(a);
    }

    @Test
    void progress_one_rep_max_sets_a_previously_absent_pattern() {
        Athlete a = reconstituted(AthleteId.generate(), 30, Rarity.GENERIC); // pas de bench
        assertThat(a.currentOneRepMax(MovementPattern.BENCH_PRESS)).isEmpty();

        Athlete progressed = a.progressOneRepMax(MovementPattern.BENCH_PRESS, OneRepMax.measured(Weight.ofKilograms(100)));

        assertThat(progressed.currentOneRepMax(MovementPattern.BENCH_PRESS).orElseThrow().weight().toKilograms().doubleValue())
                .isEqualTo(100.0);
        // Les autres patterns sont intacts (copie défensive, immutabilité).
        assertThat(progressed.currentOneRepMax(MovementPattern.SQUAT).orElseThrow().weight().toKilograms().doubleValue())
                .isEqualTo(140.0);
    }
}
