package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.util.Objects;
import java.util.Optional;

/**
 * Catégorie d'un exercice loggé (ADR-026). <strong>Sealed interface</strong> : un exercice est soit un
 * mouvement <em>composé</em> rattaché à un {@link MovementPattern} de force (squat, bench, deadlift, OHP,
 * row, chin-up), soit un <em>accessoire</em> rattaché à une {@link BodyRegion}.
 *
 * <p>Pourquoi ne pas réutiliser directement {@code MovementPattern} ? Parce qu'un accessoire n'est pas un
 * axe de force génétique : le mélanger polluerait Roster (génétique, JSONB, panel). Le type fermé permet
 * un {@code switch} exhaustif (pattern matching) côté Athletics au sprint 4 pour distribuer le stimulus :
 * {@code CompoundForce} → stimulus sur le pattern, {@code Accessory} → stimulus sur la région.
 *
 * <p>Extensible sans rien casser : ajouter {@code Cardio}, {@code Mobility}… = nouvelle variante.
 */
public sealed interface ExerciseCategory {

    /** Mouvement composé rattaché à un axe de force. */
    record CompoundForce(MovementPattern pattern) implements ExerciseCategory {
        public CompoundForce {
            Objects.requireNonNull(pattern, "pattern");
        }
    }

    /** Exercice accessoire rattaché à une région musculaire. */
    record Accessory(BodyRegion region) implements ExerciseCategory {
        public Accessory {
            Objects.requireNonNull(region, "region");
        }
    }

    static ExerciseCategory compound(MovementPattern pattern) {
        return new CompoundForce(pattern);
    }

    static ExerciseCategory accessory(BodyRegion region) {
        return new Accessory(region);
    }

    /**
     * Le {@link MovementPattern} de force si l'exercice est composé, vide sinon. Pratique pour calculer
     * les patterns couverts par une séance (les accessoires ne comptent pas — ADR-026).
     */
    default Optional<MovementPattern> movementPattern() {
        return this instanceof CompoundForce cf ? Optional.of(cf.pattern()) : Optional.empty();
    }
}
