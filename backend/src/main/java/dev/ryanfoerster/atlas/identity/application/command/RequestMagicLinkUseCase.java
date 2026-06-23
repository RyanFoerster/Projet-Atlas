package dev.ryanfoerster.atlas.identity.application.command;

import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLink;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;
import dev.ryanfoerster.atlas.identity.domain.port.EmailSender;
import dev.ryanfoerster.atlas.identity.domain.port.MagicLinkRepository;
import dev.ryanfoerster.atlas.identity.domain.service.MagicLinkExpirationPolicy;
import dev.ryanfoerster.atlas.identity.domain.service.MagicLinkTokenGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * Use case : demander un lien magique pour un email.
 *
 * <p>Orchestration applicative : valide l'email, génère un jeton, calcule l'expiration (policy),
 * émet et persiste le lien, déclenche l'envoi. <strong>Ne révèle jamais</strong> si l'email
 * existe déjà en base — toute demande bien formée renvoie le même résultat (anti-énumération
 * de comptes). C'est pour ça qu'on ne consulte même pas le {@code UserRepository} ici.
 */
@Service
public class RequestMagicLinkUseCase {

    private final MagicLinkRepository magicLinkRepository;
    private final EmailSender emailSender;
    private final MagicLinkTokenGenerator tokenGenerator;
    private final MagicLinkExpirationPolicy expirationPolicy;
    private final Clock clock;

    public RequestMagicLinkUseCase(MagicLinkRepository magicLinkRepository, EmailSender emailSender,
                                   MagicLinkTokenGenerator tokenGenerator,
                                   MagicLinkExpirationPolicy expirationPolicy, Clock clock) {
        this.magicLinkRepository = magicLinkRepository;
        this.emailSender = emailSender;
        this.tokenGenerator = tokenGenerator;
        this.expirationPolicy = expirationPolicy;
        this.clock = clock;
    }

    @Transactional
    public void request(RequestMagicLinkCommand command) {
        Email email = Email.of(command.email()); // valide / normalise (peut lever InvalidEmailException)
        Instant now = clock.instant();

        MagicLinkToken token = tokenGenerator.generate();
        MagicLink link = MagicLink.issue(token, email, now, expirationPolicy.expiresAt(now),
                command.ipAddress(), command.userAgent());
        magicLinkRepository.save(link);

        emailSender.sendMagicLink(email, token);
    }
}
