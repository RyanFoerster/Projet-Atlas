package dev.ryanfoerster.atlas.identity.application.command;

import dev.ryanfoerster.atlas.identity.api.events.PlayerLoggedIn;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.InvalidMagicLinkException;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLink;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;
import dev.ryanfoerster.atlas.identity.domain.model.User;
import dev.ryanfoerster.atlas.identity.domain.port.MagicLinkRepository;
import dev.ryanfoerster.atlas.identity.domain.port.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Use case : consommer un lien magique (le clic sur le lien reçu par email).
 *
 * <p>Déroulé : retrouver le lien, le consommer (l'entity refuse expiré/déjà-consommé), puis
 * brancher selon que le Player existe :
 * <ul>
 *   <li>existant → enregistrer le login, publier {@link PlayerLoggedIn} ;</li>
 *   <li>nouveau → ne PAS créer le compte ici (flow A) : on signale un onboarding en attente.
 *       La création se fera dans {@code CompleteSignupUseCase} après saisie du nom.</li>
 * </ul>
 *
 * <p>Note de frontière : le jeton arrive d'une URL (entrée non fiable). Un jeton mal formé fait
 * lever {@code IllegalArgumentException} par {@code MagicLinkToken.from} (erreur technique) ; on
 * la <strong>traduit</strong> ici en {@link InvalidMagicLinkException} (violation métier → 400),
 * parce qu'à cette frontière une entrée invalide n'est pas un bug mais un mauvais lien.
 */
@Service
public class ConsumeMagicLinkUseCase {

    private final MagicLinkRepository magicLinkRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public ConsumeMagicLinkUseCase(MagicLinkRepository magicLinkRepository, UserRepository userRepository,
                                   ApplicationEventPublisher eventPublisher, Clock clock) {
        this.magicLinkRepository = magicLinkRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public ConsumeResult consume(String rawToken) {
        MagicLinkToken token = parseToken(rawToken);

        MagicLink link = magicLinkRepository.findByToken(token)
                .orElseThrow(() -> new InvalidMagicLinkException("Lien magique inconnu"));

        Instant now = clock.instant();
        MagicLink consumed = link.consume(now); // lève MagicLinkNotUsableException si expiré/consommé
        magicLinkRepository.save(consumed);

        Email email = consumed.userEmail();
        Optional<User> existing = userRepository.findByEmail(email);

        if (existing.isPresent()) {
            User loggedIn = existing.get().recordLogin(now);
            userRepository.save(loggedIn);
            eventPublisher.publishEvent(new PlayerLoggedIn(loggedIn.id().value(), now));
            return ConsumeResult.existingPlayer(loggedIn.id(), email);
        }
        return ConsumeResult.pendingSignup(email);
    }

    private MagicLinkToken parseToken(String rawToken) {
        try {
            return MagicLinkToken.from(rawToken);
        } catch (IllegalArgumentException e) {
            throw new InvalidMagicLinkException("Lien magique invalide");
        }
    }
}
