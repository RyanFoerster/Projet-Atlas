package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;
import dev.ryanfoerster.atlas.shared.domain.OneRepMax;

import java.util.EnumMap;
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

    /**
     * Nouvelle instance avec le 1RM d'un pattern remplacé (progression structurelle, Couche 3). Remplacement
     * brut — la garde de monotonie (cliquet) vit dans {@code Athlete.progressOneRepMax}, qui décide s'il y a
     * lieu d'appeler cette méthode. {@link EnumMap} construit par {@code putAll} (et non par le constructeur
     * de copie, qui lèverait sur une map vide).
     */
    public CurrentStats with(MovementPattern pattern, OneRepMax oneRepMax) {
        Map<MovementPattern, OneRepMax> next = new EnumMap<>(MovementPattern.class);
        next.putAll(oneRepMaxByPattern);
        next.put(pattern, oneRepMax);
        return new CurrentStats(next);
    }
}
