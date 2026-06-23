package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Capacités structurelles de l'athlète. Version <strong>simplifiée du Sprint 2</strong> : seulement
 * les 1RM par pattern. Le Sprint 4 (Athletics) l'enrichira (masse musculaire, VO2max, % de masse
 * grasse…). Value object immutable : copie défensive de la map (décision #4).
 *
 * <p>Ne contient pas forcément tous les patterns : au Sprint 2 on a les grands lifts mesurés
 * (squat, bench, deadlift, OHP). D'où un accès en {@link Optional}.
 */
public record CurrentStats(Map<MovementPattern, OneRepMax> oneRepMaxByPattern) {

    public CurrentStats {
        Objects.requireNonNull(oneRepMaxByPattern, "oneRepMaxByPattern");
        oneRepMaxByPattern = Map.copyOf(oneRepMaxByPattern); // immutabilité réelle
    }

    public Optional<OneRepMax> oneRepMax(MovementPattern pattern) {
        return Optional.ofNullable(oneRepMaxByPattern.get(pattern));
    }
}
