package dev.ryanfoerster.atlas.athletics.domain.model;

/**
 * Représentation, côté Athletics, d'une série pour le <strong>calcul du stimulus</strong>. Au sprint 4
 * (stat globale, charge absolue hors-scope), seules deux dimensions comptent : le <em>volume</em>
 * ({@code reps}) et l'<em>intensité d'effort</em> ({@code rpe}). La charge en kg et le groupe musculaire
 * n'entrent pas dans le stimulus global (sprint 5).
 *
 * <p><strong>{@code rpe} nullable</strong> : {@code null} = RPE non renseigné. {@code StimulusCalculator}
 * le traite alors par un effort <em>neutre</em> (0.7) — voir la justification « ni bonus ni malus à
 * l'omission » dans sport-science.md. Le RPE, quand présent, est dans {@code [1.0, 10.0]} (déjà validé en
 * amont par le VO {@code RPE} de PersonalTraining lors du log ; ici on ne défend que l'incohérence de bas
 * niveau).
 */
public record SetEffort(int reps, Double rpe) {

    public SetEffort {
        if (reps < 1) {
            throw new IllegalArgumentException("Une série a au moins 1 rep : " + reps);
        }
        if (rpe != null && (rpe < 1.0 || rpe > 10.0)) {
            throw new IllegalArgumentException("RPE hors [1.0, 10.0] : " + rpe);
        }
    }
}
