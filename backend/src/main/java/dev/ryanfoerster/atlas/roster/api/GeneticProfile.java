package dev.ryanfoerster.atlas.roster.api;

/**
 * Snapshot <strong>api-level</strong> du profil génétique d'un athlète, exposé par Roster aux autres modules
 * (sprint 5, ADR-031). Types primitifs uniquement — il ne fait pas fuiter le VO {@code Genetics} interne du
 * domaine Roster (isolation Modulith).
 *
 * <p>Ne porte que les axes que <strong>Athletics</strong> consomme pour individualiser le modèle de Banister
 * (la donnée brute ; le mapping vers les paramètres Banister vit côté Athletics, ADR-031) :
 * <ul>
 *   <li>{@code baseRecoveryRate} (0.85–1.20) → module τ_fatigue (récupère vite = fatigue décroît vite) ;</li>
 *   <li>{@code trainingResponseSensitivity} (0.85–1.15) → module la magnitude du stimulus (high/low responder) ;</li>
 *   <li>{@code fiberTypeProfile} (0–1) → réservé (décidé sur simulation au GATE 3, redondance possible avec
 *       {@code baseRecoveryRate}).</li>
 * </ul>
 */
public record GeneticProfile(
        double baseRecoveryRate,
        double trainingResponseSensitivity,
        double fiberTypeProfile) {
}
