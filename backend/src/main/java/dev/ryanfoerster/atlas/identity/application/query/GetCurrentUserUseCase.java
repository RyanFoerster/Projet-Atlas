package dev.ryanfoerster.atlas.identity.application.query;

import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.model.UserId;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Use case de lecture : récupérer le Player courant à partir de son identifiant.
 *
 * <p>La couche web (S6) extrait l'{@link UserId} authentifié de la session et appelle ce use case
 * pour alimenter {@code GET /api/auth/me}. Lecture seule ({@code readOnly}).
 */
@Service
public class GetCurrentUserUseCase {

    private final UserRepository userRepository;

    public GetCurrentUserUseCase(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Optional<User> byId(UserId id) {
        return userRepository.findById(id);
    }
}
