package dev.ryanfoerster.atlas.identity.application.command;

import dev.ryanfoerster.atlas.identity.api.events.PlayerLoggedIn;
import dev.ryanfoerster.atlas.identity.api.events.PlayerRegistered;
import dev.ryanfoerster.atlas.identity.domain.model.DisplayName;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.EmailAlreadyRegisteredException;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Use case : finaliser l'inscription d'un nouveau Player (flow A) — il a vérifié son email,
 * il choisit son nom, le compte est créé et il est connecté dans la foulée.
 *
 * <p>Publie deux events : {@link PlayerRegistered} (un nouveau Player) puis {@link PlayerLoggedIn}
 * (il est connecté). L'unicité de l'email est vérifiée ici avant insertion, pour traduire une
 * éventuelle collision (double soumission) en {@link EmailAlreadyRegisteredException} claire plutôt
 * qu'en violation de contrainte SQL brute.
 */
@Service
public class CompleteSignupUseCase {

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public CompleteSignupUseCase(UserRepository userRepository, ApplicationEventPublisher eventPublisher,
                                 Clock clock) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public User completeSignup(CompleteSignupCommand command) {
        Email email = Email.of(command.verifiedEmail());
        DisplayName displayName = DisplayName.of(command.displayName());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException("Un compte existe déjà pour cet email");
        }

        Instant now = clock.instant();
        // Création puis login immédiat : finaliser l'inscription connecte le Player.
        User player = User.register(email, displayName, command.locale(), command.timezone(), now)
                .recordLogin(now);
        User saved = userRepository.save(player);

        eventPublisher.publishEvent(
                new PlayerRegistered(saved.id().value(), email.value(), displayName.value(), now));
        eventPublisher.publishEvent(new PlayerLoggedIn(saved.id().value(), now));
        return saved;
    }
}
