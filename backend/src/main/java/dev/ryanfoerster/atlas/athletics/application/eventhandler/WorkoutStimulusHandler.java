package dev.ryanfoerster.atlas.athletics.application.eventhandler;

import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.ConditionSnapshot;
import dev.ryanfoerster.atlas.athletics.domain.model.SetEffort;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.athletics.domain.port.AthleteConditionRepository;
import dev.ryanfoerster.atlas.athletics.domain.port.ConditionSnapshotRepository;
import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
import dev.ryanfoerster.atlas.athletics.domain.service.StimulusCalculator;
import dev.ryanfoerster.atlas.personaltraining.api.events.WorkoutLogged;
import dev.ryanfoerster.atlas.roster.api.RosterQueryPort;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Consumer Modulith de l'event {@link WorkoutLogged} (publié par PersonalTraining). <strong>Boucle le
 * hook central d'Atlas</strong> : la vraie séance IRL du Player fait évoluer la <em>forme</em> (Banister)
 * de son athlète <strong>miroir</strong>.
 *
 * <p><strong>Events + ports</strong> (ADR-027) : l'event ne porte que l'{@code ownerId} ; on résout le
 * miroir (clé {@code AthleteId}, option 3a) via le {@link RosterQueryPort} synchrone, puis on applique le
 * stimulus. Side-effects par event, queries par port.
 *
 * <p><strong>Idempotence</strong> : la garde {@link AthleteCondition#acceptsStimulusAt(Instant)} n'applique
 * un stimulus que si la séance est strictement postérieure au dernier état. Un rejeu (même {@code performedAt},
 * au restart ou après échec) est donc un no-op : pas de double application. La sauvegarde de la condition et
 * du snapshot se font dans la <strong>même transaction</strong> ({@code @ApplicationModuleListener} =
 * {@code REQUIRES_NEW}) → atomiques, pas de snapshot orphelin.
 *
 * <p><strong>Isolation Modulith</strong> : n'importe que {@code personaltraining.api.events} et
 * {@code roster.api} (frontières publiques), jamais le domaine interne d'un autre module.
 */
@Component
public class WorkoutStimulusHandler {

    private final RosterQueryPort rosterQueryPort;
    private final AthleteConditionRepository conditionRepository;
    private final ConditionSnapshotRepository snapshotRepository;
    private final BanisterModel banisterModel;
    private final StimulusCalculator stimulusCalculator;

    public WorkoutStimulusHandler(RosterQueryPort rosterQueryPort,
                                AthleteConditionRepository conditionRepository,
                                ConditionSnapshotRepository snapshotRepository,
                                BanisterModel banisterModel,
                                StimulusCalculator stimulusCalculator) {
        this.rosterQueryPort = rosterQueryPort;
        this.conditionRepository = conditionRepository;
        this.snapshotRepository = snapshotRepository;
        this.banisterModel = banisterModel;
        this.stimulusCalculator = stimulusCalculator;
    }

    @ApplicationModuleListener
    public void on(WorkoutLogged event) {
        Optional<AthleteId> mirror = rosterQueryPort.findMirrorAthleteId(new UserId(event.ownerId()));
        if (mirror.isEmpty()) {
            return; // le Player n'a pas d'athlète miroir → rien à faire profiter de la séance
        }
        AthleteId athleteId = mirror.get();
        Instant performedAt = event.performedAt();

        Optional<AthleteCondition> existing = conditionRepository.findByAthleteId(athleteId);
        if (existing.isPresent() && !existing.get().acceptsStimulusAt(performedAt)) {
            return; // rejeu idempotent, ou séance arrivée dans le désordre → no-op
        }

        TrainingStimulus stimulus = stimulusCalculator.from(setEfforts(event));
        AthleteCondition base = existing.orElseGet(() -> AthleteCondition.initial(athleteId, performedAt));
        AthleteCondition updated = base.applyStimulus(banisterModel, stimulus, performedAt);
        conditionRepository.save(updated);

        double performance = banisterModel.availablePerformance(updated.state());
        snapshotRepository.save(ConditionSnapshot.capture(athleteId, updated.state(), performance));
    }

    /**
     * Aplatit la séance en séries pour le calcul de stimulus : seules {@code reps} et {@code rpe} comptent
     * au sprint 4 (volume × effort, charge absolue hors-scope — ADR-028). La catégorie/le pattern de
     * l'exercice ne servent pas au stimulus <em>global</em> (ils serviront à la distribution par muscle au
     * sprint 5).
     */
    private static List<SetEffort> setEfforts(WorkoutLogged event) {
        return event.exercises().stream()
                .flatMap(exercise -> exercise.sets().stream())
                .map(set -> new SetEffort(set.reps(), set.rpe()))
                .toList();
    }
}
