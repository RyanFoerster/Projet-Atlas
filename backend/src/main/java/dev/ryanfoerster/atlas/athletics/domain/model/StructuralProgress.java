package dev.ryanfoerster.atlas.athletics.domain.model;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.util.Map;
import java.util.Optional;

/**
 * L'accumulateur de progression structurelle d'un athlète, <strong>par pattern de force</strong>
 * ({@link PatternProgress}). Value object immutable. Map <em>sparse</em> : un pattern absent = jamais
 * entraîné en composé (pas encore d'ancre/plafond), à l'image de la map par muscle du modèle de Banister.
 *
 * <p>C'est l'un des <strong>deux stores</strong> de la Couche 3 (ADR-032) : l'accumulateur chronique vit
 * côté Athletics (ici), le 1RM matérialisé (la carte de l'athlète) vit côté Roster. La progression par
 * pattern incarne le principe de spécificité (SAID) : squatter fait progresser le squat, pas le développé.
 */
public record StructuralProgress(Map<MovementPattern, PatternProgress> byPattern) {

    /** Aucun pattern encore entré en progression (athlète neuf). */
    public static final StructuralProgress EMPTY = new StructuralProgress(Map.of());

    public StructuralProgress {
        byPattern = Map.copyOf(byPattern);
    }

    /** La progression d'un pattern, ou vide s'il n'est jamais entré en progression (composé jamais loggé). */
    public Optional<PatternProgress> progress(MovementPattern pattern) {
        return Optional.ofNullable(byPattern.get(pattern));
    }
}
