package dev.ryanfoerster.atlas.roster.infrastructure.config;

import dev.ryanfoerster.atlas.roster.domain.service.AthleteGenerator;
import dev.ryanfoerster.atlas.roster.domain.service.ProceduralAthleteGenerator;
import dev.ryanfoerster.atlas.roster.domain.service.RarityRoller;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Random;

/**
 * Câblage Spring du module roster. Les domain services purs ({@link RarityRoller},
 * {@link AthleteGenerator}) deviennent des beans ici (le domaine reste sans Spring, ADR-003).
 * {@link EnableScheduling} active le job de purge des candidats scoutés (ADR-022).
 */
@Configuration
@EnableScheduling
class RosterModuleConfig {

    @Bean
    RarityRoller rarityRoller() {
        return new RarityRoller();
    }

    @Bean
    AthleteGenerator athleteGenerator() {
        return new ProceduralAthleteGenerator();
    }

    /** Source d'aléa pour le scouting (seed/roll). Bean injectable → un test peut fournir un Random seedé. */
    @Bean
    Random scoutingRandom() {
        return new Random();
    }
}
