package dev.ryanfoerster.atlas.shared.domain;

/**
 * Région musculaire ciblée par un exercice <em>accessoire</em> (cf. {@code ExerciseCategory.Accessory}).
 * Value object du <strong>kernel partagé</strong>.
 *
 * <p><strong>Promu vers {@code shared} au sprint 5</strong> (critère ADR-017 : transverse à 2+ modules —
 * PersonalTraining le produit, Athletics le consomme pour distribuer le stimulus par muscle). La promotion
 * était déjà anticipée par ADR-026. Distinct de {@link MovementPattern} : un accessoire (curl, gainage,
 * mollets) n'est pas un axe de force génétique, juste une région travaillée.
 *
 * <p>Liste volontairement compacte (besoins UX du logger), pas une taxonomie anatomique exhaustive. Le
 * mapping {@code BodyRegion → MuscleGroup} (Athletics, ADR-030) gère ses deux frictions assumées : {@code BACK}
 * (plus grossier que {@code BACK_UPPER}/{@code BACK_LOWER}) et {@code FOREARMS} (sans équivalent dans
 * {@link MuscleGroup}, replié sur {@code BICEPS}).
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
