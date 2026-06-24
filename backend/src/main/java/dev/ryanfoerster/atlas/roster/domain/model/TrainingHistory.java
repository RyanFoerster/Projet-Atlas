package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

/**
 * Trace passive de l'entraînement IRL reçu par l'athlète <strong>miroir</strong> (ADR-025). Au sprint 3
 * c'est un instantané minimal : la <em>dernière</em> séance que le miroir a "faite". Le <strong>nombre</strong>
 * de séances n'est <em>pas</em> stocké ici — sa source de vérité est PersonalTraining (option D, ADR-025),
 * interrogé via {@code PersonalTrainingQueryPort} à l'affichage. Au sprint 4, Athletics consommera ces
 * données (et l'event) pour piloter le {@code FitnessFatigueState} par {@code MuscleGroup} (Banister, ADR-004).
 *
 * <p><strong>Idempotence par écrasement monotone</strong> : {@link #recordWorkout} n'applique une séance
 * que si elle est <em>strictement plus récente</em> que la dernière connue. Rejouer un event (au restart
 * via le registry, ou après échec) ou recevoir un event plus ancien est alors un no-op — pas de double
 * comptage, pas de régression de la date. L'idempotence est garantie <em>par construction</em>, sans
 * mémoriser d'identifiant d'event (ADR-025).
 */
public record TrainingHistory(Instant lastWorkoutAt, Set<MovementPattern> lastPatternsCovered) {

    public TrainingHistory {
        Objects.requireNonNull(lastPatternsCovered, "lastPatternsCovered");
        lastPatternsCovered = Set.copyOf(lastPatternsCovered); // copie défensive → immutabilité réelle
        // lastWorkoutAt nullable : aucune séance reçue encore.
    }

    /** Historique vierge (athlète sans séance reçue — miroir neuf ou athlète virtuel). */
    public static TrainingHistory empty() {
        return new TrainingHistory(null, Set.of());
    }

    /**
     * Enregistre une séance, de façon <strong>idempotente et robuste au désordre de livraison</strong> :
     * on ne met à jour que si {@code performedAt} est strictement postérieur à {@link #lastWorkoutAt}.
     * Une séance plus ancienne ou identique (rejeu) ne change rien.
     */
    public TrainingHistory recordWorkout(Instant performedAt, Set<MovementPattern> patternsCovered) {
        Objects.requireNonNull(performedAt, "performedAt");
        if (lastWorkoutAt != null && !performedAt.isAfter(lastWorkoutAt)) {
            return this; // no-op : déjà reflété (rejeu) ou plus ancien (désordre)
        }
        return new TrainingHistory(performedAt, patternsCovered);
    }

    public boolean hasWorkouts() {
        return lastWorkoutAt != null;
    }
}
