package dev.ryanfoerster.atlas.athletics.infrastructure.persistence.json;

/**
 * Forme sérialisable (JSONB) d'un {@code PatternProgress} : départ, plafond et charge chronique accumulée
 * d'un pattern de force (Couche 3, ADR-033). Champs plats (le mapping manuel convertit vers/depuis le VO
 * domaine — le domaine ne voit jamais Jackson, ADR-015).
 */
public record PatternProgressJson(double startOneRmKg, double ceilingOneRmKg, double chronicLoad) {
}
