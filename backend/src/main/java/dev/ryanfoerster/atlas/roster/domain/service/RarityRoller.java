package dev.ryanfoerster.atlas.roster.domain.service;

import dev.ryanfoerster.atlas.roster.domain.model.Rarity;

/**
 * Domain service stateless : transforme un tirage uniforme {@code [0,1)} en tier de {@link Rarity}
 * selon les probabilités cumulées (Generic 65 % → Promising 25 % → Specialist 8 % → Prodigy 2 %).
 *
 * <p>La valeur aléatoire est <strong>passée en paramètre</strong> (jamais générée ici) : c'est ce qui
 * rend la distribution testable de façon reproductible (test « 10 000 tirages » avec un {@code Random}
 * seedé côté appelant).
 */
public final class RarityRoller {

    /**
     * @param value tirage uniforme dans {@code [0,1)}
     * @throws IllegalArgumentException si {@code value} est hors {@code [0,1)}
     */
    public Rarity roll(double value) {
        if (value < 0.0 || value >= 1.0) {
            throw new IllegalArgumentException("Le tirage doit être dans [0,1) : " + value);
        }
        double cumulative = 0.0;
        for (Rarity rarity : Rarity.values()) { // ordre : GENERIC → PROMISING → SPECIALIST → PRODIGY
            cumulative += rarity.probability();
            if (value < cumulative) {
                return rarity;
            }
        }
        return Rarity.PRODIGY; // filet de sécurité contre les arrondis flottants (cumul ≈ 1.0)
    }
}
