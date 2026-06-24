package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.EmptyWorkoutSessionException;
import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidPerformedAtException;
import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidWorkoutSessionException;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.UserId;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Une séance d'entraînement IRL loggée par le Player — <strong>aggregate root autonome</strong> du
 * module personaltraining (décision « aggregate central A »). Contrairement au {@link
 * dev.ryanfoerster.atlas.roster.domain.model.Roster Roster} (un aggregate qui contient une collection
 * d'entities), chaque séance est un aggregate à part entière : pas de « TrainingLog » géant, plus
 * scalable, pattern standard pour des entités autonomes.
 *
 * <p>Immutable (style fonctionnel comme les autres aggregates), égalité <b>par identité</b>
 * ({@link WorkoutSessionId}). La liste d'exercices est recopiée défensivement.
 *
 * <p><strong>Invariants</strong> :
 * <ul>
 *   <li>au moins un exercice ({@link EmptyWorkoutSessionException}) ;</li>
 *   <li>{@code performedAt} pas dans le futur — vérifié à la création via {@link #log} ;</li>
 *   <li>durée (si renseignée) plausible, notes ≤ 500 caractères.</li>
 * </ul>
 */
public final class WorkoutSession {

    public static final int MAX_DURATION_MINUTES = 1440; // 24 h : garde-fou anti-saisie aberrante
    public static final int MAX_NOTES_LENGTH = 500;

    private final WorkoutSessionId id;
    private final UserId ownerId;
    private final Instant performedAt;
    private final Integer durationMinutes; // nullable : non renseigné
    private final List<LoggedExercise> exercises;
    private final String notes;            // nullable : pas de notes
    private final Instant createdAt;

    private WorkoutSession(WorkoutSessionId id, UserId ownerId, Instant performedAt, Integer durationMinutes,
                           List<LoggedExercise> exercises, String notes, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId");
        this.performedAt = Objects.requireNonNull(performedAt, "performedAt");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(exercises, "exercises");
        if (exercises.isEmpty()) {
            throw new EmptyWorkoutSessionException("Une séance doit contenir au moins un exercice");
        }
        if (durationMinutes != null && (durationMinutes < 1 || durationMinutes > MAX_DURATION_MINUTES)) {
            throw new InvalidWorkoutSessionException(
                    "La durée (minutes) doit être dans [1, " + MAX_DURATION_MINUTES + "] : " + durationMinutes);
        }
        this.durationMinutes = durationMinutes;
        this.notes = normalizeNotes(notes);
        this.exercises = List.copyOf(exercises); // immutabilité réelle
    }

    private static String normalizeNotes(String notes) {
        if (notes == null) {
            return null;
        }
        String trimmed = notes.trim();
        if (trimmed.isEmpty()) {
            return null; // notes blanches = pas de notes
        }
        if (trimmed.length() > MAX_NOTES_LENGTH) {
            throw new InvalidWorkoutSessionException(
                    "Les notes ne peuvent pas dépasser " + MAX_NOTES_LENGTH + " caractères : " + trimmed.length());
        }
        return trimmed;
    }

    /**
     * Logge une nouvelle séance. {@code now} (horloge injectée par le use case — jamais
     * {@code Instant.now()} en dur) sert d'horodatage de création <em>et</em> de borne supérieure :
     * une séance ne peut pas avoir été réalisée dans le futur.
     *
     * @throws InvalidPerformedAtException si {@code performedAt} est postérieur à {@code now}
     * @throws EmptyWorkoutSessionException si {@code exercises} est vide
     */
    public static WorkoutSession log(UserId ownerId, Instant performedAt, List<LoggedExercise> exercises,
                                     Integer durationMinutes, String notes, Instant now) {
        Objects.requireNonNull(performedAt, "performedAt");
        Objects.requireNonNull(now, "now");
        if (performedAt.isAfter(now)) {
            throw new InvalidPerformedAtException(
                    "Une séance ne peut pas être dans le futur : " + performedAt + " > " + now);
        }
        return new WorkoutSession(WorkoutSessionId.generate(), ownerId, performedAt, durationMinutes,
                exercises, notes, now);
    }

    /** <strong>FOR PERSISTENCE LAYER ONLY.</strong> Réhydrate une séance (ADR-015), sans re-vérifier la
     * borne « pas dans le futur » (donnée historique déjà validée à la création). */
    public static WorkoutSession reconstitute(WorkoutSessionId id, UserId ownerId, Instant performedAt,
                                              Integer durationMinutes, List<LoggedExercise> exercises,
                                              String notes, Instant createdAt) {
        return new WorkoutSession(id, ownerId, performedAt, durationMinutes, exercises, notes, createdAt);
    }

    public int totalSets() {
        return exercises.stream().mapToInt(LoggedExercise::totalSets).sum();
    }

    public int totalReps() {
        return exercises.stream().mapToInt(LoggedExercise::totalReps).sum();
    }

    /** Volume estimé de la séance en kg : Σ (séries × reps × charge). Poids de corps = 0. */
    public double estimatedVolume() {
        return exercises.stream()
                .map(LoggedExercise::volumeKg)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                .doubleValue();
    }

    /**
     * Patterns de force couverts par la séance — <strong>uniquement</strong> les exercices composés
     * ({@link ExerciseCategory.CompoundForce}). Les accessoires n'ont pas de pattern de force et ne
     * comptent donc pas ici (ADR-026). Ordre d'insertion préservé, ensemble immutable.
     */
    public Set<MovementPattern> patternsCovered() {
        Set<MovementPattern> patterns = new LinkedHashSet<>();
        for (LoggedExercise exercise : exercises) {
            exercise.category().movementPattern().ifPresent(patterns::add);
        }
        return Set.copyOf(patterns);
    }

    public WorkoutSessionId id() {
        return id;
    }

    public UserId ownerId() {
        return ownerId;
    }

    public Instant performedAt() {
        return performedAt;
    }

    public Optional<Integer> durationMinutes() {
        return Optional.ofNullable(durationMinutes);
    }

    public List<LoggedExercise> exercises() {
        return exercises;
    }

    public Optional<String> notes() {
        return Optional.ofNullable(notes);
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof WorkoutSession other && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "WorkoutSession[id=" + id + ", owner=" + ownerId + ", performedAt=" + performedAt
                + ", exercises=" + exercises.size() + "]";
    }
}
