package dev.ryanfoerster.atlas.athletics.domain.model;

import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConditionSnapshotTest {

    private static final Instant T0 = Instant.parse("2026-01-05T18:00:00Z");
    private final AthleteId athleteId = AthleteId.generate();

    @Test
    void capture_records_the_aggregated_state_dated_at_its_last_update_and_keeps_performance_raw() {
        // Deux muscles : Σfitness = 8+4 = 12, Σfatigue = 2+1 = 3 (le snapshot est agrégé, arbitrage ④).
        FitnessFatigueState state = new FitnessFatigueState(Map.of(
                MuscleGroup.QUADS, new MuscleCondition(8.0, 2.0),
                MuscleGroup.CHEST, new MuscleCondition(4.0, 1.0)), T0);

        ConditionSnapshot snapshot = ConditionSnapshot.capture(athleteId, state, -5.0);

        assertThat(snapshot.athleteId()).isEqualTo(athleteId);
        assertThat(snapshot.takenAt()).isEqualTo(T0);
        assertThat(snapshot.fitness()).isEqualTo(12.0);
        assertThat(snapshot.fatigue()).isEqualTo(3.0);
        assertThat(snapshot.performance()).isEqualTo(-5.0); // négatif conservé (athlète « cuit »)
        assertThat(snapshot.id()).isNotNull();
    }

    @Test
    void rejects_negative_fitness_or_fatigue() {
        assertThatThrownBy(() -> new ConditionSnapshot(ConditionSnapshotId.generate(), athleteId, T0, -1.0, 0.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ConditionSnapshot(ConditionSnapshotId.generate(), athleteId, T0, 0.0, -1.0, 0.0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
