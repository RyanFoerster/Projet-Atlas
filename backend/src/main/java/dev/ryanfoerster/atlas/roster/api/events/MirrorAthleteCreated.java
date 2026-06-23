package dev.ryanfoerster.atlas.roster.api.events;

import java.time.Instant;
import java.util.UUID;

/** Event publié à la création de l'athlète miroir. Consommé typiquement par athletics/insights. */
public record MirrorAthleteCreated(UUID athleteId, UUID rosterId, Instant createdAt) {
}
