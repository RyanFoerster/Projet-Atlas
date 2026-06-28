package dev.ryanfoerster.atlas.athletics.infrastructure.persistence.json;

/**
 * Forme sérialisable (JSONB) d'une {@code MuscleConition} : la paire fitness/fatigue d'un muscle. DTO
 * d'infrastructure — il peut être touché par Jackson (Hibernate l'écrit en jsonb), contrairement au
 * domaine qui reste pur (ADR-003, ADR-015).
 */
public record MuscleConditionJson(double fitness, double fatigue) {
}
