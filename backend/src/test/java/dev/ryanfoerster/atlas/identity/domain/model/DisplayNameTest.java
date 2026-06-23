package dev.ryanfoerster.atlas.identity.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DisplayNameTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Ry",                 // borne basse (2)
            "Ryan",
            "Jean-Éloïse",        // accent + tiret
            "O'Connor",           // apostrophe droite
            "O’Connor",           // apostrophe typographique
            "Coach 123",          // chiffres + espace
            "La Fabrique du Muscle"
    })
    void accepts_valid_names(String raw) {
        assertThat(DisplayName.of(raw).value()).isEqualTo(raw);
    }

    @Test
    void trims_surrounding_whitespace() {
        assertThat(DisplayName.of("   Ryan   ").value()).isEqualTo("Ryan");
    }

    @Test
    void accepts_exactly_50_characters() {
        String name = "a".repeat(50);

        assertThat(DisplayName.of(name).value()).hasSize(50);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "a",      // 1 caractère après trim
            " x ",    // -> "x", 1 caractère
            ""        // vide
    })
    void rejects_names_too_short(String raw) {
        assertThatExceptionOfType(InvalidDisplayNameException.class).isThrownBy(() -> DisplayName.of(raw));
    }

    @Test
    void rejects_names_longer_than_50() {
        String tooLong = "a".repeat(51);

        assertThatExceptionOfType(InvalidDisplayNameException.class).isThrownBy(() -> DisplayName.of(tooLong));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Ryan@home",   // symbole
            "Ryan_Coach",  // underscore non autorisé
            "Ryan!",       // ponctuation
            "Coach\nNL",   // saut de ligne
            "💪Coach"      // emoji
    })
    void rejects_names_with_forbidden_characters(String raw) {
        assertThatExceptionOfType(InvalidDisplayNameException.class).isThrownBy(() -> DisplayName.of(raw));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Coach123",  // lettres + chiffres
            "Ryan2",     // lettres + un chiffre
            "X1"         // borne basse, mixte
    })
    void accepts_names_mixing_letters_and_digits(String raw) {
        assertThat(DisplayName.of(raw).value()).isEqualTo(raw);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "42",        // purement numérique, court
            "12345",     // purement numérique, plus long
            "0 1 - 2"    // chiffres + caractères autorisés mais aucune lettre
    })
    void rejects_purely_numeric_names(String raw) {
        assertThatExceptionOfType(InvalidDisplayNameException.class).isThrownBy(() -> DisplayName.of(raw));
    }

    @Test
    void rejects_null() {
        assertThatExceptionOfType(InvalidDisplayNameException.class).isThrownBy(() -> DisplayName.of(null));
    }

    @Test
    void equality_is_by_value() {
        assertThat(DisplayName.of("Ryan")).isEqualTo(DisplayName.of("Ryan"));
        assertThat(DisplayName.of("Ryan")).hasSameHashCodeAs(DisplayName.of("Ryan"));
    }
}
