package dev.ryanfoerster.atlas.identity.domain.service;

import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MagicLinkTokenGeneratorTest {

    @Test
    void secure_generator_produces_distinct_v7_tokens() {
        MagicLinkTokenGenerator generator = MagicLinkTokenGenerator.secure();
        Set<UUID> seen = new HashSet<>();

        for (int i = 0; i < 500; i++) {
            MagicLinkToken token = generator.generate();
            assertThat(token.value().version()).isEqualTo(7);
            assertThat(seen.add(token.value())).isTrue();
        }
    }

    @Test
    void can_be_substituted_by_a_deterministic_generator_in_tests() {
        // Démonstration du « seam » : en test on injecte un générateur déterministe.
        MagicLinkToken fixed = MagicLinkToken.generate();
        MagicLinkTokenGenerator deterministic = () -> fixed;

        assertThat(deterministic.generate()).isEqualTo(fixed);
        assertThat(deterministic.generate()).isEqualTo(fixed);
    }
}
