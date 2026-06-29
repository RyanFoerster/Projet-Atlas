package dev.ryanfoerster.atlas.personaltraining.infrastructure.web;

import dev.ryanfoerster.atlas.personaltraining.application.command.LogWorkoutCommand;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseCategory;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseName;
import dev.ryanfoerster.atlas.personaltraining.domain.model.ExerciseSet;
import dev.ryanfoerster.atlas.personaltraining.domain.model.Load;
import dev.ryanfoerster.atlas.personaltraining.domain.model.LoggedExercise;
import dev.ryanfoerster.atlas.personaltraining.domain.model.RPE;
import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidExerciseException;
import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidExerciseSetException;
import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidPerformedAtException;
import dev.ryanfoerster.atlas.personaltraining.infrastructure.web.dto.LogWorkoutDto;
import dev.ryanfoerster.atlas.shared.domain.Weight;

import java.util.List;

/**
 * Traduit le DTO web {@link LogWorkoutDto} en {@link LogWorkoutCommand} (objets du domaine). La
 * construction des VO valide à la frontière : toute violation lève une {@code DomainException} → 400.
 * Les gardes {@code null} convertissent une saisie incomplète en 400 propre (et non un NPE → 500).
 */
final class LogWorkoutDtoMapper {

    private LogWorkoutDtoMapper() {
    }

    static LogWorkoutCommand toCommand(LogWorkoutDto dto) {
        if (dto.performedAt() == null) {
            throw new InvalidPerformedAtException("performedAt est requis");
        }
        List<LogWorkoutDto.ExerciseInputDto> inputs = dto.exercises() == null ? List.of() : dto.exercises();
        List<LoggedExercise> exercises = inputs.stream().map(LogWorkoutDtoMapper::toExercise).toList();
        return new LogWorkoutCommand(dto.performedAt(), dto.durationMinutes(), dto.notes(), exercises);
    }

    private static LoggedExercise toExercise(LogWorkoutDto.ExerciseInputDto dto) {
        ExerciseCategory category = toCategory(dto);
        List<LogWorkoutDto.SetInputDto> setInputs = dto.sets() == null ? List.of() : dto.sets();
        List<ExerciseSet> sets = setInputs.stream().map(LogWorkoutDtoMapper::toSet).toList();
        return new LoggedExercise(ExerciseName.of(dto.name()), category, sets);
    }

    /** {@code pattern} XOR {@code region} : exactement l'un des deux. */
    private static ExerciseCategory toCategory(LogWorkoutDto.ExerciseInputDto dto) {
        boolean hasPattern = dto.pattern() != null;
        boolean hasRegion = dto.region() != null;
        if (hasPattern == hasRegion) {
            throw new InvalidExerciseException(
                    "Chaque exercice doit avoir soit un 'pattern' (composé) soit une 'region' (accessoire), "
                            + "pas les deux ni aucun : " + dto.name());
        }
        return hasPattern
                ? ExerciseCategory.compound(dto.pattern())
                : ExerciseCategory.accessory(dto.region());
    }

    private static ExerciseSet toSet(LogWorkoutDto.SetInputDto dto) {
        RPE rpe = dto.rpe() == null ? null : RPE.of(dto.rpe());
        return new ExerciseSet(dto.reps(), toLoad(dto), rpe);
    }

    /**
     * {@code loadType} → {@link Load}. {@code null} = compat client simple ({@code weightKg} {@code null} →
     * poids de corps, sinon externe). {@code WEIGHTED}/{@code EXTERNAL} exigent une {@code weightKg} (sinon
     * 400). Un {@code loadType} inconnu est une saisie invalide → 400.
     */
    private static Load toLoad(LogWorkoutDto.SetInputDto dto) {
        if (dto.loadType() == null) {
            return dto.weightKg() == null ? Load.bodyweight() : Load.external(Weight.ofKilograms(dto.weightKg()));
        }
        return switch (dto.loadType()) {
            case "BODYWEIGHT" -> Load.bodyweight();
            case "WEIGHTED" -> Load.weighted(requireWeight(dto));
            case "EXTERNAL" -> Load.external(requireWeight(dto));
            default -> throw new InvalidExerciseSetException("loadType inconnu : " + dto.loadType());
        };
    }

    private static Weight requireWeight(LogWorkoutDto.SetInputDto dto) {
        if (dto.weightKg() == null) {
            throw new InvalidExerciseSetException(
                    "weightKg est requis pour loadType=" + dto.loadType());
        }
        return Weight.ofKilograms(dto.weightKg());
    }
}
