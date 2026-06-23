package dev.ryanfoerster.atlas.identity.domain.port;

import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.shared.domain.UserId;

import java.util.Optional;

/**
 * Port secondaire : abstraction de persistance des {@link User}, vue depuis le domaine.
 *
 * <p>L'interface vit dans {@code domain/port/} et parle exclusivement le langage du domaine
 * (value objects, aggregate) — aucune fuite de JPA, SQL ou Spring. Son implémentation (un
 * <em>adapter</em>) vivra dans {@code infrastructure/persistence/} (S3). C'est l'inversion de
 * dépendance hexagonale : le domaine définit le contrat, l'infrastructure s'y conforme.
 */
public interface UserRepository {

    /** Persiste (insert ou update) un Player et retourne l'instance enregistrée. */
    User save(User user);

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    /** Vérifie l'unicité de l'email (invariant contrôlé à l'inscription). */
    boolean existsByEmail(Email email);
}
