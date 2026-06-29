package dev.ryanfoerster.atlas.personaltraining.infrastructure.web.dto;

import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseCategory.Accessory;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseCategory.CompoundForce;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseSet;
import dev.ryanfoerster.atlas.personaltraining.domain.model.Load;
import dev.ryanfoerster.atlas.personaltraining.domain.model.LoggedExercise;
import dev.ryanfoerster.atlas.personaltraining.domain.model.WorkoutSession;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Détail complet d'une séance (réponse de POST et de GET /sessions/:id). */
public record WorkoutSessionDto(
        String id,
        Instant performedAt,
        Integer durationMinutes,
        String notes,
        int totalSets,
        int totalReps,
        double estimatedVolumeKg,
        List<String> patternsCovered,
        List<ExerciseDto> exercises,
        Instant createdAt) {

    public static WorkoutSessionDto from(WorkoutSession session) {
        List<ExerciseDto> exercises = session.exercises().stream().map(ExerciseDto::from).toList();
        List<String> patterns = session.patternsCovered().stream().map(MovementPattern::name).toList();
        return new WorkoutSessionDto(
                session.id().toString(),
                session.performedAt(),
                session.durationMinutes().orElse(null),
                session.notes().orElse(null),
                session.totalSets(),
                session.totalReps(),
                session.estimatedVolume(),
                patterns,
                exercises,
                session.createdAt());
    }

    /** Un exercice : la catégorie sealed est aplatie en {@code category} + {@code pattern}/{@code region}. */
    public record ExerciseDto(String name, String category, String pattern, String region, List<SetDto> sets) {

        static ExerciseDto from(LoggedExercise exercise) {
            String category;
            String pattern = null;
            String region = null;
            switch (exercise.category()) {
                case CompoundForce cf -> {
                    category = "COMPOUND_FORCE";
                    pattern = cf.pattern().name();
                }
                case Accessory a -> {
                    category = "ACCESSORY";
                    region = a.region().name();
                }
            }
            List<SetDto> sets = exercise.sets().stream().map(SetDto::from).toList();
            return new ExerciseDto(exercise.name().value(), category, pattern, region, sets);
        }
    }

    /** Une série : le sealed {@code Load} est aplati en {@code loadType} + {@code weightKg} (ADR-035). */
    public record SetDto(int reps, String loadType, BigDecimal weightKg, Double rpe) {

        static SetDto from(ExerciseSet set) {
            String loadType;
            BigDecimal weightKg = null;
            switch (set.load()) {
                case Load.Bodyweight ignored -> loadType = "BODYWEIGHT";
                case Load.Weighted w -> {
                    loadType = "WEIGHTED";
                    weightKg = w.added().toKilograms();
                }
                case Load.External e -> {
                    loadType = "EXTERNAL";
                    weightKg = e.weight().toKilograms();
                }
            }
            return new SetDto(set.reps(), loadType, weightKg, set.rpe() == null ? null : set.rpe().value());
        }
    }
}
