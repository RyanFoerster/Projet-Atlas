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
