package dev.ryanfoerster.atlas.athletics.domain.model;

/**
 * État d'accumulation de la <strong>progression structurelle</strong> d'un pattern de force (le 1RM long
 * terme), dans le modèle « cible convergente + cliquet » (ADR-033). Value object immutable, la 3ᵉ échelle de
 * temps d'Atlas (mois/années) — distincte de la fitness (semaines) et de la fatigue (jours) du modèle de
 * Banister.
 *
 * <ul>
 *   <li>{@code startOneRmKg} — le 1RM au moment où le pattern entre en progression (l'<strong>ancre</strong>,
 *       figée : à charge chronique nulle, le mérité vaut le départ) ;</li>
 *   <li>{@code ceilingOneRmKg} — le <strong>plafond génétique</strong> ({@code bodyweight × ratio_ÉLITE ×
 *       strengthAffinity}, calculé par Roster, dénormalisé ici car immutable) ;</li>
 *   <li>{@code chronicLoad} — la charge d'entraînement <strong>chronique accumulée</strong> du pattern, qui
 *       décroît lentement au repos (τ ≈ 90 j) et pilote la convergence vers le plafond.</li>
 * </ul>
 *
 * <p>Le 1RM <em>mérité</em> n'est pas un champ : c'est une fonction pure de cet état, calculée par
 * {@code StructuralProgressionModel} ({@code plafond − (plafond − départ)·exp(−C/SCALE)}). Le {@code start}
 * reste figé d'une séance à l'autre (le 1RM courant monte par le cliquet, mais l'ancre du calcul ne bouge
 * pas — sinon la charge chronique serait double-comptée).
 */
public record PatternProgress(double startOneRmKg, double ceilingOneRmKg, double chronicLoad) {

    public PatternProgress {
        if (startOneRmKg <= 0.0) {
            throw new IllegalArgumentException("Le 1RM de départ doit être strictement positif : " + startOneRmKg);
        }
        if (ceilingOneRmKg < startOneRmKg) {
            throw new IllegalArgumentException(
                    "Le plafond (" + ceilingOneRmKg + ") ne peut pas être sous le départ (" + startOneRmKg + ")");
        }
        if (chronicLoad < 0.0) {
            throw new IllegalArgumentException("La charge chronique ne peut pas être négative : " + chronicLoad);
        }
    }

    /** Un pattern qui entre en progression : charge chronique nulle, mérité == départ. */
    public static PatternProgress starting(double startOneRmKg, double ceilingOneRmKg) {
        return new PatternProgress(startOneRmKg, ceilingOneRmKg, 0.0);
    }
}
