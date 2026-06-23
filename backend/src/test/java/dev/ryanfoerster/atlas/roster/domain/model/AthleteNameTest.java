package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.roster.domain.model.exceptions.InvalidAthleteNameException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class AthleteNameTest {

    @ParameterizedTest
    @ValueSource(strings = {"Marcus Vélaris", "O'Connor", "Ryan2", "La Fabrique"})
    void accepts_valid_names(String raw) {
        assertThat(AthleteName.of(raw).value()).isEqualTo(raw);
    }

    @Test
    void trims_input() {
        assertThat(AthleteName.of("  Marcus  ").value()).isEqualTo("Marcus");
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "", "12345", "Bad@Name", "💪Coach"})
    void rejects_invalid_names(String raw) {
        assertThatExceptionOfType(InvalidAthleteNameException.class).isThrownBy(() -> AthleteName.of(raw));
    }
}
