package dev.ryanfoerster.atlas.roster.infrastructure.persistence.json;

import dev.ryanfoerster.atlas.shared.domain.MovementPattern;

import java.util.Map;

/** Forme sérialisable de {@code CurrentStats}. */
public record CurrentStatsJson(Map<MovementPattern, OneRepMaxJson> oneRepMaxByPattern) {
}
