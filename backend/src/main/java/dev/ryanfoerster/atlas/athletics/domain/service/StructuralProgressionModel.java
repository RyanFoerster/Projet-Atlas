package dev.ryanfoerster.atlas.athletics.domain.service;

import dev.ryanfoerster.atlas.athletics.domain.model.PatternProgress;
import dev.ryanfoerster.atlas.athletics.domain.model.PatternStrengthRef;
import dev.ryanfoerster.atlas.athletics.domain.model.StructuralProgress;
import dev.ryanfoerster.atlas.athletics.domain.model.TrainingStimulus;
import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Dynamique de la <strong>progression structurelle du 1RM</strong> (Couche 3, ADR-033). Domain service pur
 * et stateless, parallèle au {@link BanisterModel} — mais sur la 3ᵉ échelle de temps (mois/années), celle
 * des {@code CurrentStats}, distincte de la fitness (semaines) et de la fatigue (jours).
 *
 * <h2>Cible convergente + cliquet</h2>
 * Le 1RM <em>mérité</em> par la charge chronique accumulée {@code C} converge vers un plafond génétique par
 * saturation exponentielle :
 * <pre>{@code  mérité(C) = plafond − (plafond − départ) · exp(−C / SCALE)}</pre>
 * Le <strong>cliquet</strong> est porté à la frontière d'émission ({@link #progressedOneRmKg}) : on ne
 * propage qu'une <em>hausse</em> du 1RM. Comme {@code C} décroît au repos, le mérité peut redescendre, mais
 * le 1RM matérialisé (Roster) ne recule jamais — il est le <em>max</em> des mérités passés. C'est la
 * distinction CurrentStats / Fitness rendue mécanique (un deload baisse la forme, pas le 1RM).
 *
 * <h2>Pourquoi le plafond n'est pas un magic number</h2>
 * Le plafond {@code = bodyweight × ratio_ÉLITE × strengthAffinity} réutilise les standards de force déjà
 * présents côté Roster (qui fixent le 1RM de <em>départ</em> à la borne intermédiaire). Conséquence : les
 * <strong>rendements décroissants</strong> débutant/avancé <em>émergent</em> de l'écart au plafond (un
 * débutant en est loin → progresse vite ; un avancé en est proche → se bat pour quelques kg), sans aucune
 * logique de « training age » codée.
 *
 * <h2>Stabilité</h2>
 * La boucle est stable par construction : le mérité est borné par le plafond (asymptote, jamais atteinte à
 * charge finie) et la saturation exponentielle ne peut pas s'emballer. La boucle d'auto-régulation (1RM ↑ →
 * même charge = %1RM ↓ → moins de stimulus, ADR-034) est amortissante, non divergente. Calibration
 * {@code SCALE=20} validée à l'œil de lifter sur simulation 12-16 sem (cf. test de calibration).
 *
 * <h2>Sources</h2>
 * Standards de force : ExRx.net Strength Standards ; Nuckols / Stronger By Science. Le plafond élite et la
 * borne intermédiaire (départ) sont les deux bornes de la même bande, cf. {@code ProceduralAthleteGenerator}.
 */
public final class StructuralProgressionModel {

    /** Constante de saturation : « combien de charge chronique pour approcher le plafond ». Calibrée (ADR-033 §3). */
    public static final double SCALE = 20.0;

    /** Constante de temps de la charge chronique, en jours (décroissance LENTE — 3ᵉ échelle de temps). */
    public static final double TAU_CHRONIC_DAYS = 90.0;

    private static final double SECONDS_PER_DAY = 86_400.0;

    /** 1RM <strong>mérité</strong> par la charge chronique accumulée : {@code plafond − (plafond − départ)·exp(−C/SCALE)}. */
    public double meritedOneRmKg(PatternProgress progress) {
        double gap = progress.ceilingOneRmKg() - progress.startOneRmKg();
        return progress.ceilingOneRmKg() - gap * Math.exp(-progress.chronicLoad() / SCALE);
    }

    /**
     * Décision du <strong>cliquet</strong> : le 1RM progressé à émettre s'il dépasse le 1RM courant, sinon
     * vide. Au repos, {@code C} décroît → le mérité peut passer sous le courant → on n'émet rien et le 1RM
     * tient (le cliquet ne descend jamais).
     */
    public OptionalDouble progressedOneRmKg(PatternProgress progress, double currentOneRmKg) {
        double merited = meritedOneRmKg(progress);
        return merited > currentOneRmKg ? OptionalDouble.of(merited) : OptionalDouble.empty();
    }

    /**
     * Fait avancer l'accumulateur structurel sur {@code [from, to]} : (1) décroît la charge chronique de
     * <strong>tous</strong> les patterns (le temps passe même sur les patterns non travaillés), puis (2)
     * ajoute le stimulus par pattern de cette séance. Un pattern vu pour la <strong>première fois</strong>
     * est initialisé — ancre = 1RM courant frais, plafond = plafond génétique (clampé au courant si l'athlète
     * est déjà au-delà de son potentiel). Un pattern <strong>sans référence</strong> de plafond (pas de
     * standard de force : ROW, CHIN_UP) n'est pas initialisé → ne progresse pas structurellement (simplification
     * assumée, ADR-033 §5).
     */
    public StructuralProgress advance(StructuralProgress prior,
                                      Map<MovementPattern, TrainingStimulus> byPattern,
                                      Map<MovementPattern, PatternStrengthRef> references,
                                      Instant from, Instant to) {
        double decay = Math.exp(-elapsedDays(from, to) / TAU_CHRONIC_DAYS);
        Map<MovementPattern, PatternProgress> result = new EnumMap<>(MovementPattern.class);
        prior.byPattern().forEach((pattern, p) -> result.put(pattern,
                new PatternProgress(p.startOneRmKg(), p.ceilingOneRmKg(), p.chronicLoad() * decay)));

        byPattern.forEach((pattern, stimulus) -> {
            double impulse = stimulus.magnitude();
            if (impulse <= 0.0) {
                return;
            }
            PatternProgress existing = result.get(pattern);
            if (existing != null) {
                result.put(pattern, new PatternProgress(
                        existing.startOneRmKg(), existing.ceilingOneRmKg(), existing.chronicLoad() + impulse));
                return;
            }
            PatternStrengthRef ref = references.get(pattern);
            if (ref == null) {
                return; // pas de plafond de référence → pas de progression structurelle (ROW/CHIN_UP)
            }
            // Un athlète déjà au-delà de son potentiel génétique a un plafond clampé à son 1RM courant (gap nul).
            double ceiling = Math.max(ref.ceilingOneRmKg(), ref.currentOneRmKg());
            result.put(pattern, new PatternProgress(ref.currentOneRmKg(), ceiling, impulse));
        });
        return new StructuralProgress(result);
    }

    private static double elapsedDays(Instant from, Instant to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("Le temps ne recule pas : " + to + " < " + from);
        }
        return (to.getEpochSecond() - from.getEpochSecond()) / SECONDS_PER_DAY;
    }
}
