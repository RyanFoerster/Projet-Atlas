package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.roster.domain.model.exceptions.InvalidHeightException;

/**
 * Taille corporelle en centimètres. Value object propre au module roster (Athletics fera son
 * propre besoin si nécessaire au sprint 4 — pas promu au kernel pour l'instant, ADR-017).
 *
 * <p>Bornes plausibles 120–250 cm (entrée humaine → {@link InvalidHeightException}, 400).
 */
public record Height(int centimeters) {

    private static final int MIN_CM = 120;
    private static final int MAX_CM = 250;

    public Height {
        if (centimeters < MIN_CM || centimeters > MAX_CM) {
            throw new InvalidHeightException(
                    "La taille doit être entre " + MIN_CM + " et " + MAX_CM + " cm : " + centimeters);
        }
    }

    public static Height ofCentimeters(int centimeters) {
        return new Height(centimeters);
    }

    @Override
    public String toString() {
        return centimeters + " cm";
    }
}
