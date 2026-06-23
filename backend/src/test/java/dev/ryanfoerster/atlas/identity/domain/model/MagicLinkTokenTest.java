package dev.ryanfoerster.atlas.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class MagicLinkTokenTest {

    @Test
    void generate_produces_a_version_7_uuid() {
        MagicLinkToken token = MagicLinkToken.generate();

        assertThat(token.value()).isNotNull();
        assertThat(token.value().version()).isEqualTo(7);
    }

    @Test
    void generate_produces_unique_tokens() {
        Set<UUID> seen = new HashSet<>();

        for (int i = 0; i < 1_000; i++) {
            assertThat(seen.add(MagicLinkToken.generate().value())).isTrue();
        }
    }

    @Test
    void from_parses_a_valid_uuid_string_round_trip() {
        MagicLinkToken original = MagicLinkToken.generate();

        MagicLinkToken parsed = MagicLinkToken.from(original.value().toString());

        assertThat(parsed).isEqualTo(original);
    }

    @Test
    void from_rejects_null() {
        assertThatIllegalArgumentException().isThrownBy(() -> MagicLinkToken.from(null));
    }

    @Test
    void from_rejects_malformed_string() {
        assertThatIllegalArgumentException().isThrownBy(() -> MagicLinkToken.from("nope"));
    }

    @Test
    void constructor_rejects_null_uuid() {
        assertThatIllegalArgumentException().isThrownBy(() -> new MagicLinkToken(null));
    }

    @Test
    void equality_is_by_value() {
        UUID raw = MagicLinkToken.generate().value();

        assertThat(new MagicLinkToken(raw)).isEqualTo(new MagicLinkToken(raw));
    }

    @Test
    void is_distinct_type_from_user_id_even_with_same_uuid() {
        // Garde-fou de conception : token et identifiant ne sont pas interchangeables.
        UUID raw = UUID.fromString("0190a8e0-1234-7abc-8def-0123456789ab");

        assertThat((Object) new MagicLinkToken(raw)).isNotEqualTo(new UserId(raw));
    }
}
