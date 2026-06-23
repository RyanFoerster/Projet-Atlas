package dev.ryanfoerster.atlas.roster.infrastructure.persistence.json;

import java.math.BigDecimal;

/** Forme sérialisable d'un 1RM (poids normalisé en kg + source MEASURED/ESTIMATED). */
public record OneRepMaxJson(BigDecimal weightKg, String source) {
}
