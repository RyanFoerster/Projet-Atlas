package dev.ryanfoerster.atlas.identity.domain.port;

import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;

/**
 * Port secondaire : envoi d'emails transactionnels, vu depuis le domaine.
 *
 * <p>Le port s'exprime en termes métier (un destinataire, un jeton à transmettre) et reste
 * agnostique du « comment » : la construction de l'URL du lien, le rendu HTML, le provider
 * (Resend en prod, log-only en dev — cf. ADR-013, S5) sont des détails d'infrastructure.
 * Le domaine ne sait pas, et ne doit pas savoir, comment l'email part.
 */
public interface EmailSender {

    /**
     * Envoie au destinataire le lien magique correspondant au jeton.
     *
     * @param recipient adresse validée du destinataire
     * @param token     jeton à inclure dans le lien (l'adapter construit l'URL complète)
     */
    void sendMagicLink(Email recipient, MagicLinkToken token);
}
