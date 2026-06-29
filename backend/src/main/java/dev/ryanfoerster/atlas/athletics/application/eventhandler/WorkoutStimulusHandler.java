package dev.ryanfoerster.atlas.athletics.application.eventhandler;

import dev.ryanfoerster.atlas.athletics.domain.model.AthleteCondition;
import dev.ryanfoerster.atlas.athletics.domain.model.ConditionSnapshot;
import dev.ryanfoerster.atlas.athletics.domain.model.ExerciseStimulus;
import dev.ryanfoerster.atlas.athletics.domain.model.GeneticModifiers;
import dev.ryanfoerster.atlas.athletics.domain.model.PatternStrengthRef;
import dev.ryanfoerster.atlas.athletics.domain.model.SetEffort;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.athletics.domain.port.AthleteConditionRepository;
import dev.ryanfoerster.atlas.athletics.domain.port.ConditionSnapshotRepository;
import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
import dev.ryanfoerster.atlas.athletics.domain.service.MuscleStimulusMapping;
import dev.ryanfoerster.atlas.athletics.domain.service.StimulusCalculator;
import dev.ryanfoerster.atlas.athletics.domain.service.StructuralProgressionModel;
import dev.ryanfoerster.atlas.personaltraining.api.events.ExerciseSetSnapshot;
import dev.ryanfoerster.atlas.personaltraining.api.events.LoggedExerciseSnapshot;
import dev.ryanfoerster.atlas.personaltraining.api.events.WorkoutLogged;
import dev.ryanfoerster.atlas.roster.api.AthleteLoadProfile;
import dev.ryanfoerster.atlas.roster.api.AthleteStrengthCeiling;
import dev.ryanfoerster.atlas.roster.api.RosterQueryPort;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.BodyRegion;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.MuscleGroup;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import dev.ryanfoerster.atlas.shared.events.CurrentStatsProgressed;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Consumer Modulith de l'event {@link WorkoutLogged} (publié par PersonalTraining). <strong>Boucle le hook
 * central d'Atlas</strong> : la vraie séance IRL du Player fait évoluer son athlète <strong>miroir</strong>
 * sur <strong>les trois échelles de temps</strong> — fatigue (jours), forme/fitness (semaines, Banister), et
 * désormais le <strong>1RM structurel</strong> (mois, Couche 3).
 *
 * <p><strong>La boucle d'auto-régulation se ferme ici, pour de vrai.</strong> Le handler lit le 1RM
 * <em>frais</em> (Couche 2, pour le %1RM) ET fait progresser ce même 1RM (Couche 3). Quand le 1RM monte, la
 * même charge absolue devient un %1RM plus bas → {@code loadFactor} baisse → stimulus baisse → la charge
 * chronique croît moins : boucle amortissante, stable par construction (le mérité est borné par le plafond,
 * ADR-033). La progression est <strong>matérialisée chez Roster par event</strong> (ADR-032), pas écrite
 * ici : on calcule le mérité, le cliquet décide d'émettre {@link CurrentStatsProgressed}.
 *
 * <p><strong>Idempotence</strong> : {@link AthleteCondition#acceptsStimulusAt(Instant)} n'applique un
 * stimulus que si la séance est strictement postérieure au dernier état → un rejeu est un no-op (forme ET
 * structure). L'event de progression publié l'est dans la <strong>même transaction</strong>
 * ({@code @ApplicationModuleListener} = REQUIRES_NEW) → durable via l'event publication registry (ADR-023).
 *
 * <p><strong>Isolation Modulith</strong> : n'importe que {@code personaltraining.api.events},
 * {@code roster.api} et {@code shared.events} (frontières publiques), jamais le domaine interne d'un autre
 * module.
 */
@Component
public class WorkoutStimulusHandler {

    private final RosterQueryPort rosterQueryPort;
    private final AthleteConditionRepository conditionRepository;
    private final ConditionSnapshotRepository snapshotRepository;
    private final BanisterModel banisterModel;
    private final StructuralProgressionModel structuralProgressionModel;
    private final StimulusCalculator stimulusCalculator;
    private final MuscleStimulusMapping muscleStimulusMapping;
    private final ApplicationEventPublisher eventPublisher;

    public WorkoutStimulusHandler(RosterQueryPort rosterQueryPort,
                                AthleteConditionRepository conditionRepository,
                                ConditionSnapshotRepository snapshotRepository,
                                BanisterModel banisterModel,
                                StructuralProgressionModel structuralProgressionModel,
                                StimulusCalculator stimulusCalculator,
                                MuscleStimulusMapping muscleStimulusMapping,
                                ApplicationEventPublisher eventPublisher) {
        this.rosterQueryPort = rosterQueryPort;
        this.conditionRepository = conditionRepository;
        this.snapshotRepository = snapshotRepository;
        this.banisterModel = banisterModel;
        this.structuralProgressionModel = structuralProgressionModel;
        this.stimulusCalculator = stimulusCalculator;
        this.muscleStimulusMapping = muscleStimulusMapping;
        this.eventPublisher = eventPublisher;
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
            return; // rejeu idempotent, ou séance arrivée dans le désordre → no-op (forme ET structure)
        }

        // Lectures FRAÎCHES (1RM + bodyWeight mutables, ADR-034). Le plafond, lui, est immutable mais lu ici
        // par simplicité (il ne sert qu'à initialiser un pattern jamais vu — cf. references()).
        AthleteLoadProfile loadProfile = rosterQueryPort.findLoadProfile(athleteId).orElse(AthleteLoadProfile.UNKNOWN);
        AthleteStrengthCeiling ceiling = rosterQueryPort.findStrengthCeiling(athleteId).orElse(AthleteStrengthCeiling.UNKNOWN);

        List<ExerciseStimulus> stimuli = exerciseStimuli(event, loadProfile);
        Map<MuscleGroup, TrainingStimulus> distributed = stimulusCalculator.distribute(stimuli, muscleStimulusMapping);
        Map<MovementPattern, TrainingStimulus> byPattern = stimulusCalculator.byPattern(stimuli);
        Map<MovementPattern, PatternStrengthRef> references = strengthReferences(byPattern, loadProfile, ceiling);

        // Génétique résolue UNE SEULE FOIS à la création (Genetics immutable → dénormalisée, ADR-031).
        AthleteCondition base = existing.orElseGet(() ->
                AthleteCondition.initial(athleteId, resolveGeneticModifiers(athleteId), performedAt));
        // `from` = lastUpdated AVANT la séance, capturé pour la décroissance chronique (applyStimulus l'avance).
        Instant from = base.state().lastUpdated();
        AthleteCondition updated = base
                .applyStimulus(banisterModel, distributed, performedAt)               // forme (Banister)
                .progressStructure(structuralProgressionModel, byPattern, references, from, performedAt); // structure
        conditionRepository.save(updated);

        double performance = banisterModel.availablePerformance(updated.state());
        snapshotRepository.save(ConditionSnapshot.capture(athleteId, updated.state(), performance));

        publishProgressions(athleteId, byPattern.keySet(), updated, loadProfile, performedAt);
    }

    /**
     * Références de force pour initialiser un pattern entrant en progression : 1RM courant frais + plafond
     * génétique. Seuls les patterns dotés des <strong>deux</strong> (les grands lifts) sont retenus ; un
     * composé sans 1RM/plafond suivi (ROW/CHIN_UP) est absent → il ne progresse pas (ADR-033 §5).
     */
    private static Map<MovementPattern, PatternStrengthRef> strengthReferences(
            Map<MovementPattern, TrainingStimulus> byPattern, AthleteLoadProfile loadProfile,
            AthleteStrengthCeiling ceiling) {
        Map<MovementPattern, PatternStrengthRef> references = new EnumMap<>(MovementPattern.class);
        for (MovementPattern pattern : byPattern.keySet()) {
            Double currentKg = loadProfile.oneRepMaxKg(pattern);
            Double ceilingKg = ceiling.ceilingOneRmKg(pattern);
            if (currentKg != null && currentKg > 0.0 && ceilingKg != null && ceilingKg > 0.0) {
                references.put(pattern, new PatternStrengthRef(currentKg, ceilingKg));
            }
        }
        return references;
    }

    /**
     * Émet une {@link CurrentStatsProgressed} pour chaque pattern travaillé dont le 1RM <em>mérité</em>
     * dépasse le 1RM courant (<strong>cliquet</strong> : on ne propage qu'une hausse). Roster matérialise
     * (ADR-032). Un pattern sans 1RM suivi (ROW/CHIN_UP) est ignoré.
     */
    private void publishProgressions(AthleteId athleteId, Set<MovementPattern> trainedPatterns,
                                     AthleteCondition condition, AthleteLoadProfile loadProfile, Instant performedAt) {
        for (MovementPattern pattern : trainedPatterns) {
            Double currentKg = loadProfile.oneRepMaxKg(pattern);
            if (currentKg == null) {
                continue; // pas de 1RM de référence (ROW/CHIN_UP) → pas de progression structurelle
            }
            condition.structural().progress(pattern).ifPresent(progress ->
                    structuralProgressionModel.progressedOneRmKg(progress, currentKg).ifPresent(newOneRmKg ->
                            eventPublisher.publishEvent(
                                    new CurrentStatsProgressed(athleteId.value(), pattern, newOneRmKg, performedAt))));
        }
    }

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

    private static List<ExerciseStimulus> exerciseStimuli(WorkoutLogged event, AthleteLoadProfile loadProfile) {
        return event.exercises().stream()
                .map(exercise -> toExerciseStimulus(exercise, loadProfile))
                .toList();
    }

    /**
     * Traduit un exercice loggé en {@link ExerciseStimulus} domaine : sa <em>cible</em> (pattern composé ou
     * région accessoire) et ses séries (reps × rpe × %1RM). Le %1RM = {@code charge totale / 1RM du pattern}
     * (ADR-034) ; il est {@code null} pour un accessoire ou un composé sans 1RM connu → {@code loadFactor} au
     * plancher.
     */
    private static ExerciseStimulus toExerciseStimulus(LoggedExerciseSnapshot exercise, AthleteLoadProfile loadProfile) {
        // 1RM du pattern, seulement pour un composé (un accessoire n'a pas d'axe de force / de 1RM).
        Double oneRepMaxKg = exercise.pattern() != null ? loadProfile.oneRepMaxKg(exercise.pattern()) : null;
        List<SetEffort> sets = exercise.sets().stream()
                .map(set -> new SetEffort(set.reps(), set.rpe(),
                        percentOneRepMax(set, oneRepMaxKg, loadProfile.bodyWeightKg())))
                .toList();
        // Frontière anti-corruption : l'event porte le nom de région en String (ADR-024, types primitifs) ;
        // Athletics le résout en BodyRegion (shared) pour son mapping typé. Le nom vient toujours de
        // BodyRegion.name() côté producteur → valueOf est sûr.
        return exercise.pattern() != null
                ? ExerciseStimulus.compound(exercise.pattern(), sets)
                : ExerciseStimulus.accessory(BodyRegion.valueOf(exercise.accessoryRegion()), sets);
    }

    /** %1RM d'une série = charge totale / 1RM ; {@code null} si pas de 1RM de référence (→ loadFactor plancher). */
    private static Double percentOneRepMax(ExerciseSetSnapshot set, Double oneRepMaxKg, double bodyWeightKg) {
        if (oneRepMaxKg == null || oneRepMaxKg <= 0.0) {
            return null;
        }
        return totalLoadKg(set.loadType(), set.weightKg(), bodyWeightKg) / oneRepMaxKg;
    }

    /**
     * Charge totale déplacée (kg), résolue depuis la saisie typée (Couche 1, ADR-035) + le poids de corps :
     * {@code EXTERNAL} → charge externe seule ; {@code WEIGHTED} → poids de corps + leste ; {@code BODYWEIGHT}
     * → poids de corps seul. Un {@code loadType} absent (legacy défensif) est traité comme une charge brute.
     */
    private static double totalLoadKg(String loadType, Double weightKg, double bodyWeightKg) {
        double value = weightKg == null ? 0.0 : weightKg;
        if (loadType == null) {
            return value; // legacy : pas de type → charge brute (cohérent avec le lecteur tolérant)
        }
        return switch (loadType) {
            case ExerciseSetSnapshot.WEIGHTED -> bodyWeightKg + value;
            case ExerciseSetSnapshot.BODYWEIGHT -> bodyWeightKg;
            case ExerciseSetSnapshot.EXTERNAL -> value;
            default -> value;
        };
    }
}
