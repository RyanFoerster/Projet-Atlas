package dev.ryanfoerster.atlas.athletics.domain.model;

/**
 * Représentation, côté Athletics, d'une série pour le <strong>calcul du stimulus</strong>. Trois dimensions
 * de dose :
 * <ul>
 *   <li><strong>volume</strong> = {@code reps} ;</li>
 *   <li><strong>intensité d'effort</strong> = {@code rpe} (proximité de l'échec) ;</li>
 *   <li><strong>intensité de charge</strong> = {@code percentOneRepMax} (tension mécanique, %1RM) — entrée
 *       au sprint 6 (ADR-034). {@code charge totale / 1RM du pattern}, résolu par le handler depuis la saisie
 *       (poids de corps / lesté / externe) + le 1RM lu <em>frais</em> dans Roster.</li>
 * </ul>
 *
 * <p><strong>Nullables (« ni bonus ni malus à l'omission ») :</strong>
 * <ul>
 *   <li>{@code rpe} {@code null} → effort neutre (RPE 7) dans {@code StimulusCalculator} ;</li>
 *   <li>{@code percentOneRepMax} {@code null} → <strong>pas de 1RM de référence</strong> (accessoire, ou
 *       composé sans 1RM connu) → {@code loadFactor} au plancher. Les axes effort/charge sont
 *       <em>orthogonaux</em> : un accessoire garde son crédit d'effort (RPE), pas son crédit de tension.</li>
 * </ul>
 * Le constructeur 2-arg délègue avec {@code percentOneRepMax = null} (série sans référence de charge).
 */
public record SetEffort(int reps, Double rpe, Double percentOneRepMax) {

    public SetEffort {
        if (reps < 1) {
            throw new IllegalArgumentException("Une série a au moins 1 rep : " + reps);
        }
        if (rpe != null && (rpe < 1.0 || rpe > 10.0)) {
            throw new IllegalArgumentException("RPE hors [1.0, 10.0] : " + rpe);
        }
        if (percentOneRepMax != null && percentOneRepMax < 0.0) {
            throw new IllegalArgumentException("Le %1RM ne peut pas être négatif : " + percentOneRepMax);
        }
    }

    /** Série sans référence de charge (accessoire / 1RM inconnu) : {@code loadFactor} au plancher. */
    public SetEffort(int reps, Double rpe) {
        this(reps, rpe, null);
    }
}
