package dev.ryanfoerster.atlas.athletics.domain.model;

import dev.ryanfoerster.atlas.shared.domain.AthleteId;

import java.time.Instant;
import java.util.Objects;

/**
 * Point daté de la trajectoire Fitness/Fatigue d'un athlète, capturé <strong>à chaque séance appliquée</strong>
 * (event {@code WorkoutLogged}). Append-only : les snapshots ne sont jamais modifiés, ils alimenteront les
 * <strong>courbes du sprint 7 (Insights)</strong>. Value object immutable.
 *
 * <p>{@code performance} peut être <strong>négative</strong> (athlète « cuit » juste après une grosse
 * séance) — on conserve la valeur brute du modèle ; la normalisation 0–100 est faite à l'affichage.
 */
public record ConditionSnapshot(
        ConditionSnapshotId id,
        AthleteId athleteId,
        Instant takenAt,
        double fitness,
        double fatigue,
        double performance) {

    public ConditionSnapshot {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(athleteId, "athleteId");
        Objects.requireNonNull(takenAt, "takenAt");
        if (fitness < 0) {
            throw new IllegalArgumentException("fitness négative : " + fitness);
        }
        if (fatigue < 0) {
            throw new IllegalArgumentException("fatigue négative : " + fatigue);
        }
    }

    /**
     * Capture l'état courant d'une condition comme snapshot daté ({@code takenAt = state.lastUpdated()}).
     * <strong>Agrégé</strong> (sprint 5, arbitrage ④) : on stocke la fitness/fatigue <em>sommées</em> sur
     * les muscles — c'est la tendance globale dont les courbes du sprint 7 ont besoin, pas le détail par
     * muscle. La performance disponible (agrégée) est calculée en amont par le {@code BanisterModel}.
     */
    public static ConditionSnapshot capture(AthleteId athleteId, FitnessFatigueState state, double performance) {
        return new ConditionSnapshot(ConditionSnapshotId.generate(), athleteId, state.lastUpdated(),
                state.totalFitness(), state.totalFatigue(), performance);
    }
}
