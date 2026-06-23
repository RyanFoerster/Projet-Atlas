package dev.ryanfoerster.atlas.identity.infrastructure.config;

import dev.ryanfoerster.atlas.identity.domain.service.MagicLinkExpirationPolicy;
import dev.ryanfoerster.atlas.identity.domain.service.MagicLinkTokenGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Câblage Spring du module identity.
 *
 * <p>Les domain services ({@link MagicLinkExpirationPolicy}, {@link MagicLinkTokenGenerator})
 * sont des classes <em>pures</em> (zéro annotation Spring, ADR-003) : on les transforme en beans
 * ici, dans l'infrastructure, plutôt que de les annoter {@code @Component} — c'est ce qui garde
 * le domaine indépendant du framework.
 *
 * <p>Le {@link Clock} est injecté partout où un use case a besoin de « maintenant », pour rester
 * testable (un test injecte {@code Clock.fixed(...)}). On ne fait jamais {@code Instant.now()}
 * en dur.
 */
@Configuration
class IdentityModuleConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    MagicLinkExpirationPolicy magicLinkExpirationPolicy() {
        return new MagicLinkExpirationPolicy();
    }

    @Bean
    MagicLinkTokenGenerator magicLinkTokenGenerator() {
        return MagicLinkTokenGenerator.secure();
    }
}
