package dev.ryanfoerster.atlas.athletics.infrastructure.config;

import dev.ryanfoerster.atlas.athletics.domain.service.BanisterModel;
import dev.ryanfoerster.atlas.athletics.domain.service.MuscleStimulusMapping;
import dev.ryanfoerster.atlas.athletics.domain.service.StimulusCalculator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Câblage Spring du module athletics. Les domain services purs ({@link BanisterModel},
 * {@link StimulusCalculator}, {@link MuscleStimulusMapping}) deviennent des beans ici — le domaine reste
 * sans Spring (ADR-003). Stateless et sans état mutable : un singleton partagé convient.
 */
@Configuration
class AthleticsModuleConfig {

    @Bean
    BanisterModel banisterModel() {
        return new BanisterModel();
    }

    @Bean
    StimulusCalculator stimulusCalculator() {
        return new StimulusCalculator();
    }

    @Bean
    MuscleStimulusMapping muscleStimulusMapping() {
        return new MuscleStimulusMapping();
    }
}
