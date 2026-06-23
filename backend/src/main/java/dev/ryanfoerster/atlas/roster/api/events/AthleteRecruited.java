package dev.ryanfoerster.atlas.roster.api.events;

import java.time.Instant;
import java.util.UUID;

/** Event publié quand un athlète virtuel est recruté dans une écurie. */
public record AthleteRecruited(UUID athleteId, UUID rosterId, String rarity, Instant recruitedAt) {
}
