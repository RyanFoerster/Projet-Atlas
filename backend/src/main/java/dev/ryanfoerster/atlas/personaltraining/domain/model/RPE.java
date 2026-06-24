package dev.ryanfoerster.atlas.personaltraining.domain.model;

import dev.ryanfoerster.atlas.personaltraining.domain.model.exceptions.InvalidRPEException;

/**
 * RPE — <em>Rate of Perceived Exertion</em> (échelle d'effort perçu de Tuchscherer / Helms).
 * Value object auto-validant : valeur dans [1.0, 10.0] par incréments de 0.5 (ex. 7.5, 8, 8.5).
 *
 * <p>Introduit au sprint 3 dans {@code personaltraining.domain}. S'il devient transverse (Athletics
 * en aura probablement besoin au sprint 4), il sera promu au kernel {@code shared} selon le critère
 * d'ADR-017 (transverse à 2+ modules ET fondamental). Anti-dette : on ne sur-anticipe pas.
 *
 * <p>Le pas de 0.5 est représentable exactement en {@code double} (0.5, 1.0, …), donc l'égalité par
 * valeur du record est fiable ici — pas besoin de {@link java.math.BigDecimal}.
 */
public record RPE(double value) {

    public static final double MIN = 1.0;
    public static final double MAX = 10.0;
    private static final double STEP = 0.5;

    public RPE {
        if (value < MIN || value > MAX) {
            throw new InvalidRPEException("Le RPE doit être dans [" + MIN + ", " + MAX + "] : " + value);
        }
        double doubled = value / STEP;
        if (Math.abs(doubled - Math.rint(doubled)) > 1e-9) {
            throw new InvalidRPEException("Le RPE doit être un incrément de " + STEP + " : " + value);
        }
    }

    public static RPE of(double value) {
        return new RPE(value);
    }
}
