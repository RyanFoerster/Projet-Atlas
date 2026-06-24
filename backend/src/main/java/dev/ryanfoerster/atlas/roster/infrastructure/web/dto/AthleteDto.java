package dev.ryanfoerster.atlas.roster.infrastructure.web.dto;

import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fiche détaillée d'un athlète (réponse de GET /api/roster/athletes/:id, POST mirror/recruit). */
public record AthleteDto(
        String id,
        String name,
        int age,
        BigDecimal bodyWeightKg,
        int bodyHeightCm,
        String gender,
        String rarity,
        boolean mirror,
        GeneticsDto genetics,
        Map<String, BigDecimal> oneRepMaxesKg,
        Instant recruitedAt,
        TrainingHistoryDto trainingHistory) {

    /**
     * @param workoutCount nombre de séances IRL — composé côté backend depuis PersonalTraining (option D,
     *                     ADR-025). Pertinent pour le miroir ; 0 pour un athlète virtuel au sprint 3.
     */
    public static AthleteDto from(Athlete a, long workoutCount) {
        Map<String, BigDecimal> orms = new LinkedHashMap<>();
        a.currentStats().oneRepMaxByPattern()
                .forEach((pattern, orm) -> orms.put(pattern.name(), orm.weight().toKilograms()));
        return new AthleteDto(a.id().toString(), a.name().value(), a.age(), a.bodyWeight().toKilograms(),
                a.bodyHeight().centimeters(), a.gender().name(), a.rarity().name(), a.isMirror(),
                GeneticsDto.from(a.genetics()), orms, a.recruitedAt(),
                TrainingHistoryDto.from(a, workoutCount));
    }

    /**
     * Historique d'entraînement pour la fiche. {@code workoutCount} vient de PersonalTraining (source de
     * vérité) ; {@code lastWorkoutAt}/{@code lastPatternsCovered} viennent du {@code TrainingHistory} du
     * miroir (trace locale pour l'affichage et, sprint 4, le stimulus).
     */
    public record TrainingHistoryDto(long workoutCount, Instant lastWorkoutAt, List<String> lastPatternsCovered) {

        static TrainingHistoryDto from(Athlete a, long workoutCount) {
            List<String> patterns = a.trainingHistory().lastPatternsCovered().stream()
                    .map(MovementPattern::name).sorted().toList();
            return new TrainingHistoryDto(workoutCount, a.trainingHistory().lastWorkoutAt(), patterns);
        }
    }
}
