package dev.ryanfoerster.atlas.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository Spring Data sur l'entité JPA {@link UserJpaEntity}. Détail d'infrastructure :
 * il n'est jamais exposé au domaine, il est utilisé par {@link UserPersistenceAdapter} qui
 * implémente le port {@code UserRepository}.
 */
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmail(String email);
}
