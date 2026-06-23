package dev.ryanfoerster.atlas;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Beans réellement transverses (utilisés par plusieurs modules), définis au niveau de l'application
 * — pas dans un module. {@link Clock} en est l'exemple : identity ET roster (et les suivants) en ont
 * besoin pour rester testables (jamais {@code Instant.now()} en dur). Le définir ici évite des beans
 * {@code Clock} concurrents par module.
 *
 * <p>Vit dans le package racine {@code dev.ryanfoerster.atlas} (la « coquille » applicative), pas dans
 * un module — donc neutre vis-à-vis de l'isolation Modulith.
 */
@Configuration
class GlobalBeansConfig {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
