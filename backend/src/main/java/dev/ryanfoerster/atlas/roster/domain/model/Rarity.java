package dev.ryanfoerster.atlas.roster.domain.model;

/**
 * Tier de rareté d'un athlète. <strong>La rareté est de la spécialisation, pas du niveau global</strong>
 * (ADR-020) : un {@code PRODIGY} est exceptionnel sur <em>un</em> axe précis (un pattern de force ou
 * un groupe musculaire), avec les autres axes variables — jamais « 99 partout ». Un {@code GENERIC}
 * est équilibré autour de la moyenne.
 *
 * <p>Les probabilités (somme = 1.0) pilotent le tirage de scouting via {@code RarityRoller}.
 * L'ordre de déclaration va du plus commun au plus rare (utilisé pour le tirage cumulatif).
 */
public enum Rarity {
    GENERIC(0.65),
    PROMISING(0.25),
    SPECIALIST(0.08),
    PRODIGY(0.02);

    private final double probability;

    Rarity(double probability) {
        this.probability = probability;
    }

    public double probability() {
        return probability;
    }
}
