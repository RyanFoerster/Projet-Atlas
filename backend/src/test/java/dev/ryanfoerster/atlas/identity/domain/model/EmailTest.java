package dev.ryanfoerster.atlas.identity.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;

class EmailTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "ryan@example.com",
            "ryan.foerster@example.co.uk",
            "r@x.io",
            "user+tag@sub.domain.org",
            "a_b-c%d@my-domain.com",
            "123@456.dev"
    })
    void accepts_valid_emails(String raw) {
        assertThat(Email.of(raw).value()).isEqualTo(raw);
    }

    @Test
    void normalizes_trim_and_lowercase() {
        Email email = Email.of("  Ryan.Foerster@Example.COM  ");

        assertThat(email.value()).isEqualTo("ryan.foerster@example.com");
    }

    @Test
    void same_address_with_different_casing_is_equal() {
        assertThat(Email.of("RYAN@EXAMPLE.COM")).isEqualTo(Email.of("ryan@example.com"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "plainaddress",        // pas de @
            "@no-local.com",       // pas de partie locale
            "no-domain@",          // pas de domaine
            "no-tld@domain",       // pas de TLD
            "two@@at.com",         // double @
            "spaces in@email.com", // espace interne
            "trailing@dot.",       // TLD vide
            "a@b.c"                // TLD 1 caractère
    })
    void rejects_invalid_emails(String raw) {
        assertThatExceptionOfType(InvalidEmailException.class).isThrownBy(() -> Email.of(raw));
    }

    @Test
    void rejects_null() {
        assertThatExceptionOfType(InvalidEmailException.class).isThrownBy(() -> Email.of(null));
    }

    @Test
    void rejects_blank() {
        assertThatExceptionOfType(InvalidEmailException.class).isThrownBy(() -> Email.of("   "));
    }

    @Test
    void rejects_address_longer_than_254_chars() {
        String local = "a".repeat(250);
        String tooLong = local + "@x.com"; // > 254

        assertThatExceptionOfType(InvalidEmailException.class).isThrownBy(() -> Email.of(tooLong));
    }

    @Test
    void canonical_constructor_rejects_non_normalized_value() {
        // construire directement (sans passer par of()) un email non normalisé doit échouer :
        // l'invariant est porté par le constructeur, pas seulement par la factory.
        assertThatExceptionOfType(InvalidEmailException.class)
                .isThrownBy(() -> new Email("UPPERCASE@example.com"));
    }

    /**
     * Test « fuzzing » randomisé — remplace temporairement le property-based test jqwik
     * (différé au Sprint 4, cf. CLAUDE.md §2 et rétro Sprint 0). Sur 1000 entrées aléatoires,
     * on vérifie la propriété de <em>totalité</em> de {@code Email.of} : pour n'importe quelle
     * chaîne, soit elle lève {@link InvalidEmailException}, soit elle renvoie un Email
     * normalisé et valide — jamais une exception technique (NPE, IndexOutOfBounds…).
     *
     * <p>Seed fixe pour la reproductibilité (un échec est rejouable à l'identique).
     */
    @Test
    void of_is_total_on_random_input() {
        Random random = new Random(42);
        String alphabet = "abcAB12@.-_ +%é@@..  ";

        for (int i = 0; i < 1_000; i++) {
            String raw = randomString(random, alphabet);

            Throwable thrown = catchThrowable(() -> Email.of(raw));

            if (thrown != null) {
                // seule l'exception métier est tolérée
                assertThat(thrown).isInstanceOf(InvalidEmailException.class);
            } else {
                Email email = Email.of(raw);
                // si accepté : normalisé (trim + lowercase) et de longueur valide
                assertThat(email.value()).isEqualTo(raw.trim().toLowerCase());
                assertThat(email.value()).hasSizeLessThanOrEqualTo(254);
                assertThat(email.value()).contains("@");
            }
        }
    }

    private static String randomString(Random random, String alphabet) {
        int length = random.nextInt(30); // 0..29, couvre le cas vide
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
