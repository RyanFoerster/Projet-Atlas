package dev.ryanfoerster.atlas.athletics.domain.model;

/**
 * Référence de force d'un pattern, lue côté Roster et passée au {@code StructuralProgressionModel} pour
 * <strong>initialiser</strong> une progression : le 1RM courant (l'ancre de départ, lu <em>frais</em> car
 * mutable) et le plafond génétique (calculé par Roster depuis les standards de force × {@code
 * strengthAffinity}, immutable donc dénormalisable). ADR-033 (T3, T5) : on lit frais ce qui change (le 1RM
 * courant), on dénormalise ce qui ne change pas (le plafond).
 *
 * <p>Le plafond peut être <em>sous</em> le 1RM courant si l'athlète a déjà dépassé son potentiel génétique
 * (cas du miroir au 1RM IRL élevé) : le modèle clampe alors le plafond au courant (gap nul → pas de
 * progression), d'où l'absence d'invariant {@code ceiling ≥ current} ici.
 */
public record PatternStrengthRef(double currentOneRmKg, double ceilingOneRmKg) {

    public PatternStrengthRef {
        if (currentOneRmKg <= 0.0) {
            throw new IllegalArgumentException("Le 1RM courant doit être strictement positif : " + currentOneRmKg);
        }
        if (ceilingOneRmKg <= 0.0) {
            throw new IllegalArgumentException("Le plafond doit être strictement positif : " + ceilingOneRmKg);
        }
    }
}
