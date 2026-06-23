package dev.ryanfoerster.atlas.identity.domain.service;

import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;

/**
 * Domain service stateless qui produit les jetons de liens magiques.
 *
 * <p>Modélisé comme une interface (point d'extension / « seam ») bien que la production se
 * contente de déléguer à {@link MagicLinkToken#generate()}. L'intérêt est la <b>testabilité</b> :
 * un use case qui dépend de cette interface peut, en test, recevoir un générateur déterministe
 * (jeton fixe) pour écrire des assertions stables — sans dépendre de l'aléa.
 *
 * <p>Interface fonctionnelle : un test peut fournir l'implémentation via une simple lambda.
 */
@FunctionalInterface
public interface MagicLinkTokenGenerator {

    MagicLinkToken generate();

    /** Implémentation de production : un nouvel UUID v7 à chaque appel. */
    static MagicLinkTokenGenerator secure() {
        return MagicLinkToken::generate;
    }
}
