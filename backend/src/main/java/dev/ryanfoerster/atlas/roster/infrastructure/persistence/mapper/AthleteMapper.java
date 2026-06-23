package dev.ryanfoerster.atlas.roster.infrastructure.persistence.mapper;

import dev.ryanfoerster.atlas.roster.domain.model.Athlete;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteId;
import dev.ryanfoerster.atlas.roster.domain.model.AthleteName;
import dev.ryanfoerster.atlas.roster.domain.model.Gender;
import dev.ryanfoerster.atlas.roster.domain.model.Height;
import dev.ryanfoerster.atlas.roster.domain.model.Rarity;
import dev.ryanfoerster.atlas.roster.domain.model.RosterId;
import dev.ryanfoerster.atlas.roster.infrastructure.persistence.AthleteJpaEntity;
import dev.ryanfoerster.atlas.roster.infrastructure.persistence.json.RosterJsonConverter;
import dev.ryanfoerster.atlas.shared.domain.Weight;

/**
 * Helper INTERNE de {@link RosterMapper} (Athlete = entity interne au Roster, ADR-019 : pas de mapper
 * indépendant utilisé par d'autres couches). Conversions Athlete ↔ JPA, y compris les blobs JSONB
 * (genetics/currentStats via {@link RosterJsonConverter}).
 */
final class AthleteMapper {

    private AthleteMapper() {
    }

    static AthleteJpaEntity toEntity(Athlete a) {
        AthleteJpaEntity e = new AthleteJpaEntity();
        e.setId(a.id().value());
        e.setName(a.name().value());
        e.setAge(a.age());
        e.setBodyWeightKg(a.bodyWeight().toKilograms());
        e.setBodyHeightCm(a.bodyHeight().centimeters());
        e.setGender(a.gender().name());
        e.setRarity(a.rarity().name());
        e.setMirror(a.isMirror());
        e.setGenetics(RosterJsonConverter.toJson(a.genetics()));
        e.setCurrentStats(RosterJsonConverter.toJson(a.currentStats()));
        e.setRecruitedAt(a.recruitedAt());
        return e; // le lien vers le roster est posé par RosterMapper via addAthlete(...)
    }

    static Athlete toDomain(AthleteJpaEntity e, RosterId rosterId) {
        return Athlete.reconstitute(
                new AthleteId(e.getId()), rosterId, AthleteName.of(e.getName()), e.getAge(),
                Weight.ofKilograms(e.getBodyWeightKg()), Height.ofCentimeters(e.getBodyHeightCm()),
                Gender.valueOf(e.getGender()), RosterJsonConverter.fromJson(e.getGenetics()),
                RosterJsonConverter.fromJson(e.getCurrentStats()), Rarity.valueOf(e.getRarity()),
                e.isMirror(), e.getRecruitedAt());
    }
}
