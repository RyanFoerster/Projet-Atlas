package dev.ryanfoerster.atlas.personaltraining.infrastructure.web.dto;

import dev.ryanfoerster.atlas.shared.domain.BodyRegion;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Corps de {@code POST /api/personal-training/sessions}. Les champs {@code pattern} / {@code region}
 * sont typés en enums : Jackson renvoie un 400 automatiquement sur une valeur inconnue.
 *
 * <p><strong>Catégorie d'un exercice = {@code pattern} XOR {@code region}</strong> : exactement l'un des
 * deux doit être fourni. {@code pattern} → mouvement composé (axe de force), {@code region} → accessoire.
 * Fournir les deux ou aucun est une erreur (→ 400, levée à la construction du domaine).
 */
public record LogWorkoutDto(
        Instant performedAt,
        Integer durationMinutes,
        String notes,
        List<ExerciseInputDto> exercises) {

    public record ExerciseInputDto(
            String name,
            MovementPattern pattern,
            BodyRegion region,
            List<SetInputDto> sets) {
    }

    public record SetInputDto(int reps, BigDecimal weightKg, Double rpe) {
    }
}
