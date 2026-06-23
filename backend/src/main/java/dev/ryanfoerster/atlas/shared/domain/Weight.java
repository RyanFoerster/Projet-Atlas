package dev.ryanfoerster.atlas.shared.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Poids, avec son unité. Value object du kernel partagé (transverse : poids de corps, charges,
 * 1RM… — ADR-017). Encapsule un {@link BigDecimal} (précision) et une unité pour empêcher les
 * confusions d'unités et les valeurs négatives.
 *
 * <p>Un poids négatif est une incohérence de bas niveau (jamais un input métier valide) →
 * {@link IllegalArgumentException}. La validation de plages métier (ex. poids de corps plausible)
 * relève des aggregates qui consomment ce VO, pas du VO lui-même.
 */
public record Weight(BigDecimal value, Unit unit) {

    public enum Unit {
        KG,
        LB
    }

    private static final BigDecimal LB_TO_KG = new BigDecimal("0.45359237");

    public Weight {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(unit, "unit");
        if (value.signum() < 0) {
            throw new IllegalArgumentException("Un poids ne peut pas être négatif : " + value);
        }
    }

    public static Weight ofKilograms(double kilograms) {
        return new Weight(BigDecimal.valueOf(kilograms), Unit.KG);
    }

    public static Weight ofKilograms(BigDecimal kilograms) {
        return new Weight(kilograms, Unit.KG);
    }

    /** Valeur convertie en kilogrammes (unité de référence interne pour comparaisons/calculs). */
    public BigDecimal toKilograms() {
        return unit == Unit.KG ? value : value.multiply(LB_TO_KG);
    }
}
