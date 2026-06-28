package dev.ryanfoerster.atlas.athletics.application.eventhandler;

import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.ConditionSnapshot;
import dev.ryanfoerster.atlas.athletics.domain.model.ExerciseStimulus;
import dev.ryanfoerster.atlas.athletics.domain.model.GeneticModifiers;
import dev.ryanfoerster.atlas.athletics.domain.model.SetEffort;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.athletics.domain.port.AthleteConditionRepository;
import dev.ryanfoerster.atlas.athletics.domain.port.ConditionSnapshotRepository;
import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
import dev.ryanfoerster.atlas.athletics.domain.service.MuscleStimulusMapping;
import dev.ryanfoerster.atlas.athletics.domain.service.StimulusCalculator;
import dev.ryanfoerster.atlas.personaltraining.api.events.LoggedExerciseSnapshot;
import dev.ryanfoerster.atlas.personaltraining.api.events.WorkoutLogged;
import dev.ryanfoerster.atlas.roster.api.RosterQueryPort;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.BodyRegion;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
    private final MuscleStimulusMapping muscleStimulusMapping;

    public WorkoutStimulusHandler(RosterQueryPort rosterQueryPort,
                                AthleteConditionRepository conditionRepository,
                                ConditionSnapshotRepository snapshotRepository,
                                BanisterModel banisterModel,
                                StimulusCalculator stimulusCalculator,
                                MuscleStimulusMapping muscleStimulusMapping) {
        this.rosterQueryPort = rosterQueryPort;
        this.conditionRepository = conditionRepository;
        this.snapshotRepository = snapshotRepository;
        this.banisterModel = banisterModel;
        this.stimulusCalculator = stimulusCalculator;
        this.muscleStimulusMapping = muscleStimulusMapping;
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

        Map<MuscleGroup, TrainingStimulus> distributed =
                stimulusCalculator.distribute(exerciseStimuli(event), muscleStimulusMapping);
        // Génétique résolue UNE SEULE FOIS, à la création de la condition (Genetics immutable → dénormalisée).
        // Les séances suivantes réutilisent les modifiers stockés ; pas d'appel Roster superflu (ADR-031).
        AthleteCondition base = existing.orElseGet(() ->
                AthleteCondition.initial(athleteId, resolveGeneticModifiers(athleteId), performedAt));
        AthleteCondition updated = base.applyStimulus(banisterModel, distributed, performedAt);
        conditionRepository.save(updated);

        double performance = banisterModel.availablePerformance(updated.state());
        snapshotRepository.save(ConditionSnapshot.capture(athleteId, updated.state(), performance));
    }

    /**
     * Traduit chaque exercice loggé en {@link ExerciseStimulus} domaine : sa <em>cible</em> (pattern composé
     * ou région accessoire) et ses séries (reps × rpe). Le {@link StimulusCalculator} en déduit la magnitude
     * et la distribue sur les muscles via le {@link MuscleStimulusMapping}. La charge absolue reste
     * hors-scope (sprint 6, ADR-028).
     */
    /**
     * Résout les {@link GeneticModifiers} de l'athlète via le port Roster (mapping {@code Genetics →}
     * paramètres Banister, ADR-031). Athlète sans profil (cas théorique) → {@link GeneticModifiers#NEUTRAL}.
     * Appelé seulement à la création de la condition (cf. {@code orElseGet}).
     */
    private GeneticModifiers resolveGeneticModifiers(AthleteId athleteId) {
        return rosterQueryPort.findGeneticProfile(athleteId)
                .map(profile -> new GeneticModifiers(profile.baseRecoveryRate(), profile.trainingResponseSensitivity()))
                .orElse(GeneticModifiers.NEUTRAL);
    }

    private static List<ExerciseStimulus> exerciseStimuli(WorkoutLogged event) {
        return event.exercises().stream().map(WorkoutStimulusHandler::toExerciseStimulus).toList();
    }

    private static ExerciseStimulus toExerciseStimulus(LoggedExerciseSnapshot exercise) {
        List<SetEffort> sets = exercise.sets().stream()
                .map(set -> new SetEffort(set.reps(), set.rpe()))
                .toList();
        // Frontière anti-corruption : l'event porte le nom de région en String (ADR-024, types primitifs) ;
        // Athletics le résout en BodyRegion (shared) pour son mapping typé. Le nom vient toujours de
        // BodyRegion.name() côté producteur → valueOf est sûr.
        return exercise.pattern() != null
                ? ExerciseStimulus.compound(exercise.pattern(), sets)
                : ExerciseStimulus.accessory(BodyRegion.valueOf(exercise.accessoryRegion()), sets);
    }
}
