package dev.ryanfoerster.atlas.athletics.infrastructure.web.dto;

import dev.ryanfoerster.atlas.athletics.application.query.GetAthleteConditionUseCase.CurrentCondition;
import dev.ryanfoerster.atlas.shared.domain.AthleteId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Mapping de présentation : indice de Forme 0–100 (50 = neutre) + état cuit/frais/affûté. */
class AthleteConditionDtoTest {

    private static final Instant NOW = Instant.parse("2026-01-05T18:00:00Z");
    private final AthleteId id = AthleteId.generate();

    private CurrentCondition condition(double fitness, double fatigue, double performance) {
        return new CurrentCondition(id, fitness, fatigue, performance, NOW);
    }

    @Test
    void fully_recovered_athlete_is_affute_near_100() {
        // fatigue nulle → performance = fitness → ratio 1 → indice 100.
        AthleteConditionDto dto = AthleteConditionDto.from(condition(20.0, 0.0, 20.0));

        assertThat(dto.formIndex()).isEqualTo(100);
        assertThat(dto.formState()).isEqualTo(AthleteConditionDto.AFFUTE);
    }

    @Test
    void freshly_trained_athlete_is_cuit_clamped_at_zero() {
        // fitness = fatigue → performance = (k1−k2)·fitness < 0 → ratio −1 → indice 0.
        AthleteConditionDto dto = AthleteConditionDto.from(condition(20.0, 20.0, -20.0));

        assertThat(dto.formIndex()).isEqualTo(0);
        assertThat(dto.formState()).isEqualTo(AthleteConditionDto.CUIT);
    }

    @Test
    void neutral_balance_maps_to_fifty_frais() {
        // performance = 0 → ratio 0 → indice 50.
        AthleteConditionDto dto = AthleteConditionDto.from(condition(20.0, 10.0, 0.0));

        assertThat(dto.formIndex()).isEqualTo(50);
        assertThat(dto.formState()).isEqualTo(AthleteConditionDto.FRAIS);
    }

    @Test
    void an_athlete_without_data_reads_neutral_not_cuit() {
        AthleteConditionDto dto = AthleteConditionDto.from(condition(0.0, 0.0, 0.0));

        assertThat(dto.formIndex()).isEqualTo(50);
        assertThat(dto.formState()).isEqualTo(AthleteConditionDto.FRAIS);
    }
}
