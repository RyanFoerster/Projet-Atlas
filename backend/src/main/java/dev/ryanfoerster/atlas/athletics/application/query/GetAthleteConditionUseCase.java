package dev.ryanfoerster.atlas.athletics.application.query;

import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.FitnessFatigueState;
import dev.ryanfoerster.atlas.athletics.domain.port.AthleteConditionRepository;
import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Use case de lecture de la condition d'un athlète, avec <strong>lazy compute</strong> (ADR-006) : on ne
 * tick jamais l'athlète ; on décroît son état stocké jusqu'à « maintenant » au moment de la query.
 *
 * <p>Athlète sans condition (jamais entraîné) → état neutre (fitness/fatigue à zéro). On ne projette pas
 * vers un instant antérieur au {@code lastUpdated} (séance future éventuelle) : la décroissance ne recule
 * pas dans le temps.
 */
@Service
public class GetAthleteConditionUseCase {

    private final AthleteConditionRepository repository;
    private final BanisterModel banisterModel;
    private final Clock clock;

    public GetAthleteConditionUseCase(AthleteConditionRepository repository, BanisterModel banisterModel,
                                      Clock clock) {
        this.repository = repository;
        this.banisterModel = banisterModel;
        this.clock = clock;
    }

    public CurrentCondition forAthlete(AthleteId athleteId) {
        Instant now = clock.instant();
        Optional<AthleteCondition> condition = repository.findByAthleteId(athleteId);
        FitnessFatigueState state = condition
                .map(c -> {
                    Instant base = c.state().lastUpdated();
                    Instant asOf = now.isBefore(base) ? base : now; // pas de décroissance « vers le passé »
                    return c.projectedTo(banisterModel, asOf);
                })
                .orElseGet(() -> FitnessFatigueState.initial(now));
        double performance = banisterModel.availablePerformance(state);
        return new CurrentCondition(athleteId, state.fitness(), state.fatigue(), performance, state.lastUpdated());
    }

    /** Condition courante projetée à la lecture. {@code performance} peut être négative (athlète « cuit »). */
    public record CurrentCondition(AthleteId athleteId, double fitness, double fatigue, double performance,
                                   Instant asOf) {
    }
}
