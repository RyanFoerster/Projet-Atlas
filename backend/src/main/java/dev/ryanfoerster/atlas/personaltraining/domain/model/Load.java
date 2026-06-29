package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.shared.domain.Weight;

import java.util.Objects;
import java.util.Optional;

/**
 * Type de charge d'une série (sprint 6, couche 1, ADR-035). <strong>Sealed interface</strong> : une série
 * est soit au <em>poids de corps</em> pur ({@link Bodyweight}), soit <em>lestée</em> ({@link Weighted} =
 * poids de corps + charge ajoutée), soit en <em>charge externe</em> ({@link External}). Value object
 * auto-validant qui rend les états illégaux irreprésentables : un poids de corps ne porte pas de valeur,
 * un lesté/externe en porte forcément une (non-null, non-négative via {@link Weight}).
 *
 * <p>Pourquoi un sealed plutôt qu'un {@code enum + Weight nullable} ? Pour la même raison qu'on a un sealed
 * {@code ExerciseCategory} : un discriminant + un champ nullable peuvent se désynchroniser (un
 * {@code BODYWEIGHT} avec une valeur, un {@code EXTERNAL} sans). Le type fermé interdit ces états à la
 * compilation et permet un {@code switch} exhaustif côté frontières (event, JSON, DTO).
 *
 * <h2>Frontière de responsabilité</h2>
 * PersonalTraining <strong>ne calcule pas la charge totale déplacée</strong> : le poids de corps de
 * l'athlète vit dans Roster, pas ici. Son rôle est de <em>logger fidèlement la saisie</em> (le type + la
 * valeur ajoutée/externe). La résolution « charge totale = poids de corps (+ leste) » et le %1RM sont
 * faits côté Athletics (sprint 6, couche 2), seul module qui lit le bodyweight et le 1RM.
 */
public sealed interface Load {

    /** Poids de corps pur (traction, dips, pompes au poids de corps). Aucune charge externe saisie. */
    record Bodyweight() implements Load {
    }

    /** Lesté : poids de corps + une charge ajoutée (traction +40 kg, dips lesté). */
    record Weighted(Weight added) implements Load {
        public Weighted {
            Objects.requireNonNull(added, "added");
        }
    }

    /** Charge externe seule (squat à la barre, développé couché, tirage). */
    record External(Weight weight) implements Load {
        public External {
            Objects.requireNonNull(weight, "weight");
        }
    }

    /** Singleton du poids de corps pur (immuable, sans état). */
    Load BODYWEIGHT = new Bodyweight();

    static Load bodyweight() {
        return BODYWEIGHT;
    }

    static Load weighted(Weight added) {
        return new Weighted(added);
    }

    static Load external(Weight weight) {
        return new External(weight);
    }

    /**
     * La charge <em>externe</em> portée par la série : la charge ajoutée pour un lesté, la charge externe
     * pour un externe, vide au poids de corps pur. <strong>Ce n'est PAS la charge totale déplacée</strong>
     * (qui inclurait le poids de corps) — ce calcul appartient à Athletics. Pratique pour le volume externe
     * estimé côté PersonalTraining (le poids de corps n'y est pas modélisé comme charge).
     */
    default Optional<Weight> externalWeight() {
        return switch (this) {
            case Bodyweight ignored -> Optional.empty();
            case Weighted w -> Optional.of(w.added());
            case External e -> Optional.of(e.weight());
        };
    }
}
