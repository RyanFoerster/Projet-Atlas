package dev.ryanfoerster.atlas.roster.infrastructure.persistence;

import dev.ryanfoerster.atlas.roster.domain.model.Roster;
import dev.ryanfoerster.atlas.roster.domain.model.RosterId;
import dev.ryanfoerster.atlas.roster.domain.port.RosterRepository;
import dev.ryanfoerster.atlas.roster.infrastructure.persistence.mapper.RosterMapper;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import dev.ryanfoerster.atlas.shared.domain.UserId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adapter secondaire : implémente {@link RosterRepository} via Spring Data + {@link RosterMapper}.
 *
 * <p>{@code @Transactional} sur les lectures : la collection {@code athletes} est LAZY, le mapper la
 * parcourt pour reconstruire l'aggregate — il faut donc une session ouverte. Le Roster de domaine
 * retourné est un objet plein (aucun proxy lazy ne fuit hors de la transaction).
 */
@Component
public class RosterPersistenceAdapter implements RosterRepository {

    private final RosterJpaRepository jpaRepository;
    private final RosterMapper mapper;

    public RosterPersistenceAdapter(RosterJpaRepository jpaRepository, RosterMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Roster save(Roster roster) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(roster)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Roster> findById(RosterId id) {
        return jpaRepository.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Roster> findByOwnerId(UserId ownerId) {
        return jpaRepository.findByOwnerId(ownerId.value()).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Roster> findByAthleteId(AthleteId athleteId) {
        return jpaRepository.findByAthleteId(athleteId.value()).map(mapper::toDomain);
    }
}
