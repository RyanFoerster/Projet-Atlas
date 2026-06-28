package dev.ryanfoerster.atlas.athletics.domain.model;

/**
 * Modificateurs du modèle de Banister dérivés de la génétique d'un athlète (sprint 5, Couche 3, ADR-031).
 * Value object immutable, <strong>dénormalisé dans {@code AthleteCondition}</strong> : résolu une seule fois
 * à la création de la condition (la {@code Genetics} est immutable → zéro divergence), pas à chaque séance.
 *
 * <ul>
 *   <li>{@code recoveryRate} (génétique {@code baseRecoveryRate}, 0.85–1.20) → module τ_fatigue :
 *       {@code τ_fatigue_eff = τ_fatigue / recoveryRate}. Récupère vite ⇒ fatigue décroît vite ⇒
 *       supercompense plus vite. N'affecte <strong>pas</strong> τ_fitness (arbitrage : court terme).</li>
 *   <li>{@code stimulusMultiplier} (génétique {@code trainingResponseSensitivity}, 0.85–1.15) → module la
 *       magnitude du stimulus (high/low responder, HERITAGE/Bouchard). Interprété comme un multiplicateur
 *       déterministe de réponse, pas un bruit stochastique (revérification de l'axe, ADR-031).</li>
 * </ul>
 *
 * <p>Le mapping {@code Genetics → GeneticModifiers} vit dans la couche application d'Athletics (le handler),
 * pour garder ce VO pur et sans dépendance à {@code roster.api}.
 */
public record GeneticModifiers(double recoveryRate, double stimulusMultiplier) {

    /** Aucun effet génétique : τ_fatigue de base, magnitude inchangée. */
    public static final GeneticModifiers NEUTRAL = new GeneticModifiers(1.0, 1.0);

    public GeneticModifiers {
        if (recoveryRate <= 0) {
            throw new IllegalArgumentException("recoveryRate doit être > 0 : " + recoveryRate);
        }
        if (stimulusMultiplier < 0) {
            throw new IllegalArgumentException("stimulusMultiplier ne peut pas être négatif : " + stimulusMultiplier);
        }
    }
}
