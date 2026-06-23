package dev.ryanfoerster.atlas.identity.infrastructure.persistence;

import dev.ryanfoerster.atlas.identity.domain.model.MagicLink;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;
import dev.ryanfoerster.atlas.identity.domain.port.MagicLinkRepository;
import dev.ryanfoerster.atlas.identity.infrastructure.persistence.mapper.MagicLinkPersistenceMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter secondaire : implémente le port {@link MagicLinkRepository} via Spring Data
 * ({@link MagicLinkJpaRepository}) et {@link MagicLinkPersistenceMapper}.
 */
@Component
public class MagicLinkPersistenceAdapter implements MagicLinkRepository {

    private final MagicLinkJpaRepository jpaRepository;
    private final MagicLinkPersistenceMapper mapper;

    public MagicLinkPersistenceAdapter(MagicLinkJpaRepository jpaRepository, MagicLinkPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public MagicLink save(MagicLink magicLink) {
        MagicLinkJpaEntity saved = jpaRepository.save(mapper.toEntity(magicLink));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<MagicLink> findByToken(MagicLinkToken token) {
        return jpaRepository.findById(token.value()).map(mapper::toDomain);
    }
}
