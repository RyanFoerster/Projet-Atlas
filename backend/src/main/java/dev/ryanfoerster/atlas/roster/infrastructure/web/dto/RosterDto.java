package dev.ryanfoerster.atlas.roster.infrastructure.web.dto;

import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.roster.domain.model.Roster;

import java.math.BigDecimal;
import java.util.List;

/** L'écurie (réponse de GET /api/roster) : athlètes en résumé. */
public record RosterDto(String id, boolean hasMirror, List<AthleteSummaryDto> athletes) {

    public static RosterDto from(Roster roster) {
        List<AthleteSummaryDto> summaries = roster.athletes().stream().map(AthleteSummaryDto::from).toList();
        return new RosterDto(roster.id().toString(), roster.hasMirror(), summaries);
    }

    /** Résumé d'un athlète pour la grille. */
    public record AthleteSummaryDto(String id, String name, String rarity, boolean mirror, int age,
                                    BigDecimal bodyWeightKg) {
        static AthleteSummaryDto from(Athlete a) {
            return new AthleteSummaryDto(a.id().toString(), a.name().value(), a.rarity().name(),
                    a.isMirror(), a.age(), a.bodyWeight().toKilograms());
        }
    }
}
