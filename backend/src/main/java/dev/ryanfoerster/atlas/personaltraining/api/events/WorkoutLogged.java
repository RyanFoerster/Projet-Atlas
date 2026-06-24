package dev.ryanfoerster.atlas.personaltraining.api.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event public : une séance vient d'être loggée par un Player. Publié par PersonalTraining (dans la
 * transaction qui persiste la séance), consommé par Roster pour mettre à jour le {@code TrainingHistory}
 * de l'athlète miroir, et plus tard par Athletics (sprint 4) pour appliquer le stimulus.
 *
 * <p><strong>Autosuffisant</strong> (décision « event WorkoutLogged B ») : il transporte tout ce dont un
 * consumer a besoin (owner, séance, exercices aplatis) pour qu'il n'ait jamais à re-query
 * PersonalTraining. Types <strong>primitifs / shared</strong> uniquement (UUID, {@code Instant}, snapshots) —
 * aucun type du domaine interne (isolation Modulith, ADR-024). Les ids sont des {@link UUID} nus (même
 * convention que les events Roster), pas les VO d'identité.
 */
public record WorkoutLogged(
        UUID ownerId,
        UUID sessionId,
        Instant performedAt,
        Integer durationMinutes,
        List<LoggedExerciseSnapshot> exercises) {
}
