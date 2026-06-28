package dev.ryanfoerster.atlas.athletics.application.query;

import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.GeneticModifiers;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.athletics.domain.port.AthleteConditionRepository;
import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests du <strong>lazy compute</strong> avec un {@link Clock} fixe (déterministe, sans Testcontainers) :
 * l'état est projeté à la volée jusqu'à « maintenant » depuis l'état stocké.
 */
class GetAthleteConditionUseCaseTest {

    private static final Instant T0 = Instant.parse("2026-01-05T18:00:00Z");
    private final BanisterModel model = new BanisterModel();
    private final AthleteId id = AthleteId.generate();

    private GetAthleteConditionUseCase useCaseAt(AthleteCondition stored, Instant now) {
        AthleteConditionRepository repository = new AthleteConditionRepository() {
            @Override
            public AthleteCondition save(AthleteCondition condition) {
                return condition;
            }

            @Override
            public Optional<AthleteCondition> findByAthleteId(AthleteId athleteId) {
                return Optional.ofNullable(stored);
            }
        };
        return new GetAthleteConditionUseCase(repository, model, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void an_athlete_without_a_condition_reads_as_neutral() {
        var current = useCaseAt(null, T0).forAthlete(id);

        assertThat(current.fitness()).isZero();
        assertThat(current.fatigue()).isZero();
        assertThat(current.performance()).isZero();
    }

    @Test
    void resting_decays_fitness_and_fatigue_and_improves_available_performance() {
        AthleteCondition trained = AthleteCondition.initial(id, GeneticModifiers.NEUTRAL, T0)
                .applyStimulus(model, Map.of(MuscleGroup.QUADS, new TrainingStimulus(20.0)), T0); // f = fat = 20

        var immediate = useCaseAt(trained, T0).forAthlete(id);
        var aWeekLater = useCaseAt(trained, T0.plus(Duration.ofDays(7))).forAthlete(id);

        // Repos : les deux décroissent...
        assertThat(aWeekLater.fitness()).isLessThan(immediate.fitness());
        assertThat(aWeekLater.fatigue()).isLessThan(immediate.fatigue());
        // ...mais la fatigue (τ court) décroît davantage en relatif → la performance disponible remonte.
        assertThat(aWeekLater.fatigue() / immediate.fatigue())
                .isLessThan(aWeekLater.fitness() / immediate.fitness());
        assertThat(immediate.performance()).isNegative();              // « cuit » juste après la séance
        assertThat(aWeekLater.performance()).isGreaterThan(immediate.performance()); // récupération
    }

    @Test
    void a_future_dated_state_is_not_decayed_backwards() {
        Instant future = T0.plus(Duration.ofDays(10));
        AthleteCondition futureState = AthleteCondition.initial(id, GeneticModifiers.NEUTRAL, future)
                .applyStimulus(model, Map.of(MuscleGroup.QUADS, new TrainingStimulus(15.0)), future);

        // now (T0) est antérieur au lastUpdated → on ne décroît pas « vers le passé », pas d'exception.
        var current = useCaseAt(futureState, T0).forAthlete(id);

        assertThat(current.fitness()).isEqualTo(15.0);
        assertThat(current.fatigue()).isEqualTo(15.0);
    }
}
