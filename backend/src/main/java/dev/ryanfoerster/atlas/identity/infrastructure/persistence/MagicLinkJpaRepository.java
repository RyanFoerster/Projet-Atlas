package dev.ryanfoerster.atlas.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Repository Spring Data sur {@link MagicLinkJpaEntity}. La recherche par jeton se fait via
 * {@code findById} hérité (le token est la clé primaire). Utilisé par
 * {@link MagicLinkPersistenceAdapter}.
 */
public interface MagicLinkJpaRepository extends JpaRepository<MagicLinkJpaEntity, UUID> {
}
