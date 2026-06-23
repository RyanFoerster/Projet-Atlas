package dev.ryanfoerster.atlas.identity.infrastructure.persistence.mapper;

import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLink;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;
import dev.ryanfoerster.atlas.identity.infrastructure.persistence.MagicLinkJpaEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper manuel entre l'entity {@link MagicLink} et {@link MagicLinkJpaEntity} (ADR-015).
 * Reconstruction via {@link MagicLink#reconstitute} (réhydrate y compris l'état consommé).
 */
@Component
public class MagicLinkPersistenceMapper {

    public MagicLinkJpaEntity toEntity(MagicLink link) {
        MagicLinkJpaEntity entity = new MagicLinkJpaEntity();
        entity.setToken(link.token().value());
        entity.setUserEmail(link.userEmail().value());
        entity.setCreatedAt(link.createdAt());
        entity.setExpiresAt(link.expiresAt());
        entity.setConsumedAt(link.consumedAt().orElse(null));
        entity.setIpAddress(link.ipAddress().orElse(null));
        entity.setUserAgent(link.userAgent().orElse(null));
        return entity;
    }

    public MagicLink toDomain(MagicLinkJpaEntity entity) {
        return MagicLink.reconstitute(
                new MagicLinkToken(entity.getToken()),
                Email.of(entity.getUserEmail()),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getConsumedAt(),
                entity.getIpAddress(),
                entity.getUserAgent());
    }
}
