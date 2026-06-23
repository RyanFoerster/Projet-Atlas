package dev.ryanfoerster.atlas.roster.domain.model;

import dev.ryanfoerster.atlas.roster.domain.model.exceptions.InvalidHeightException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class HeightTest {

    @ParameterizedTest
    @ValueSource(ints = {120, 178, 250})
    void accepts_plausible_heights(int cm) {
        assertThat(Height.ofCentimeters(cm).centimeters()).isEqualTo(cm);
    }

    @ParameterizedTest
    @ValueSource(ints = {119, 251, 0, -5})
    void rejects_implausible_heights(int cm) {
        assertThatExceptionOfType(InvalidHeightException.class).isThrownBy(() -> Height.ofCentimeters(cm));
    }
}
