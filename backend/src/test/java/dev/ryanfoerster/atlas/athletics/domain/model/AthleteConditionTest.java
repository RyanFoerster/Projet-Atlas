package dev.ryanfoerster.atlas.athletics.domain.model;

import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** TDD de l'aggregate {@link AthleteCondition} : transitions d'état (déléguées au {@link BanisterModel}) et garde d'idempotence. */
class AthleteConditionTest {

    private static final Instant T0 = Instant.parse("2026-01-05T18:00:00Z");
    private final BanisterModel model = new BanisterModel();
    private final AthleteId athleteId = AthleteId.generate();

    @Test
    void initial_condition_is_neutral() {
        AthleteCondition condition = AthleteCondition.initial(athleteId, T0);

        assertThat(condition.state().fitness()).isZero();
        assertThat(condition.state().fatigue()).isZero();
        assertThat(condition.state().lastUpdated()).isEqualTo(T0);
    }

    @Test
    void applying_a_stimulus_raises_fitness_and_fatigue_and_advances_the_clock() {
        AthleteCondition updated = AthleteCondition.initial(athleteId, T0)
                .applyStimulus(model, new TrainingStimulus(10.0), T0);

        assertThat(updated.state().fitness()).isEqualTo(10.0);
        assertThat(updated.state().fatigue()).isEqualTo(10.0);
        assertThat(updated.state().lastUpdated()).isEqualTo(T0);
        assertThat(updated.athleteId()).isEqualTo(athleteId);
    }

    @Test
    void projecting_forward_decays_without_mutating_the_condition() {
        AthleteCondition condition = AthleteCondition.initial(athleteId, T0)
                .applyStimulus(model, new TrainingStimulus(10.0), T0);

        FitnessFatigueState projected = condition.projectedTo(model, T0.plus(Duration.ofDays(7)));

        assertThat(projected.fatigue()).isLessThan(projected.fitness()); // fatigue décroît plus vite
        assertThat(condition.state().fitness()).isEqualTo(10.0); // l'original n'a pas bougé (immutabilité)
    }

    @Test
    void accepts_a_stimulus_only_if_strictly_later_than_the_last_state() {
        AthleteCondition condition = AthleteCondition.initial(athleteId, T0)
                .applyStimulus(model, new TrainingStimulus(5.0), T0);

        assertThat(condition.acceptsStimulusAt(T0.plus(Duration.ofDays(1)))).isTrue();  // postérieur
        assertThat(condition.acceptsStimulusAt(T0)).isFalse();                          // rejeu (même instant)
        assertThat(condition.acceptsStimulusAt(T0.minus(Duration.ofDays(1)))).isFalse(); // dans le désordre
    }

    @Test
    void equality_is_by_athlete_identity() {
        AthleteCondition a = AthleteCondition.initial(athleteId, T0);
        AthleteCondition b = AthleteCondition.initial(athleteId, T0).applyStimulus(model, new TrainingStimulus(99.0), T0);

        assertThat(a).isEqualTo(b); // même athleteId → égaux, quel que soit l'état
    }
}
