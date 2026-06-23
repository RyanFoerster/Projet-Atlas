package dev.ryanfoerster.atlas.identity.infrastructure.config;

import dev.ryanfoerster.atlas.identity.domain.service.MagicLinkExpirationPolicy;
import dev.ryanfoerster.atlas.identity.domain.service.MagicLinkTokenGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Câblage Spring du module identity.
 *
 * <p>Les domain services ({@link MagicLinkExpirationPolicy}, {@link MagicLinkTokenGenerator})
 * sont des classes <em>pures</em> (zéro annotation Spring, ADR-003) : on les transforme en beans
 * ici, dans l'infrastructure, plutôt que de les annoter {@code @Component} — c'est ce qui garde
 * le domaine indépendant du framework.
 *
 * <p>Le {@code Clock} (transverse) est désormais un bean global ({@code GlobalBeansConfig}).
 */
@Configuration
class IdentityModuleConfig {

    @Bean
    MagicLinkExpirationPolicy magicLinkExpirationPolicy() {
        return new MagicLinkExpirationPolicy();
    }

    @Bean
    MagicLinkTokenGenerator magicLinkTokenGenerator() {
        return MagicLinkTokenGenerator.secure();
    }
}
