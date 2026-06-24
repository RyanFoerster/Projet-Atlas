package dev.ryanfoerster.atlas.personaltraining.domain.model;

/**
 * Région musculaire ciblée par un exercice <em>accessoire</em> (cf. {@link ExerciseCategory.Accessory}).
 *
 * <p>Volontairement distinct de {@link dev.ryanfoerster.atlas.shared.domain.MovementPattern} : un
 * accessoire (curl, gainage, mollets) n'est pas un axe de force génétique, juste une région travaillée
 * (ADR-026). Enum <strong>interne au module</strong> au sprint 3 ; promotion possible vers {@code shared}
 * au sprint 4 si Athletics en a besoin pour distribuer le stimulus (critère ADR-017).
 *
 * <p>Liste volontairement compacte (besoins UX du logger), pas une taxonomie anatomique exhaustive.
 *
 * <p><strong>Mapping vers {@link dev.ryanfoerster.atlas.shared.domain.MuscleGroup} (sprint 4)</strong> :
 * volontairement distinct. ~9/11 en correspondance directe, mais deux frictions à arbitrer quand
 * Athletics calculera le stimulus d'hypertrophie : {@code BACK} est plus grossier que
 * {@code BACK_UPPER}/{@code BACK_LOWER}, et {@code FOREARMS} n'a pas d'équivalent dans {@code MuscleGroup}.
 * Décision repoussée au sprint 4 (traçabilité, cf. ADR-026).
 */
public enum BodyRegion {
    BICEPS,
    TRICEPS,
    SHOULDERS,
    CHEST,
    BACK,
    FOREARMS,
    CORE,
    GLUTES,
    HAMSTRINGS,
    QUADS,
    CALVES
}
