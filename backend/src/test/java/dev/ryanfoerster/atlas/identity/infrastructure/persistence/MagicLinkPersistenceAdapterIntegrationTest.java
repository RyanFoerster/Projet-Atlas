package dev.ryanfoerster.atlas.identity.infrastructure.persistence;

import dev.ryanfoerster.atlas.AbstractIntegrationTest;
import dev.ryanfoerster.atlas.identity.domain.model.Email;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLink;
import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;
import dev.ryanfoerster.atlas.identity.domain.port.MagicLinkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MagicLinkPersistenceAdapterIntegrationTest extends AbstractIntegrationTest {

    private static final Instant CREATED = Instant.parse("2026-06-23T10:00:00.123456Z");
    private static final Instant EXPIRES = CREATED.plus(Duration.ofMinutes(15));

    @Autowired
    private MagicLinkRepository magicLinkRepository;

    @Autowired
    private MagicLinkJpaRepository jpaRepository;

    @Test
    void saves_and_finds_an_unconsumed_link_by_token() {
        MagicLinkToken token = MagicLinkToken.generate();
        MagicLink link = MagicLink.issue(token, Email.of("ryan@example.com"), CREATED, EXPIRES,
                "127.0.0.1", "JUnit");

        magicLinkRepository.save(link);
        Optional<MagicLink> found = magicLinkRepository.findByToken(token);

        assertThat(found).isPresent();
        MagicLink reloaded = found.orElseThrow();
        assertThat(reloaded).isEqualTo(link);                       // égalité par token
        assertThat(reloaded.userEmail()).isEqualTo(Email.of("ryan@example.com"));
        assertThat(reloaded.createdAt()).isEqualTo(CREATED);
        assertThat(reloaded.expiresAt()).isEqualTo(EXPIRES);
        assertThat(reloaded.isConsumed()).isFalse();
        assertThat(reloaded.ipAddress()).contains("127.0.0.1");
        assertThat(reloaded.userAgent()).contains("JUnit");
    }

    @Test
    void persists_consumption_state() {
        MagicLinkToken token = MagicLinkToken.generate();
        MagicLink link = MagicLink.issue(token, Email.of("ryan@example.com"), CREATED, EXPIRES, null, null);
        magicLinkRepository.save(link);

        Instant consumedAt = CREATED.plusSeconds(60);
        magicLinkRepository.save(link.consume(consumedAt));

        MagicLink reloaded = magicLinkRepository.findByToken(token).orElseThrow();
        assertThat(reloaded.isConsumed()).isTrue();
        assertThat(reloaded.consumedAt()).contains(consumedAt);
    }

    @Test
    void returns_empty_for_an_unknown_token() {
        assertThat(magicLinkRepository.findByToken(MagicLinkToken.generate())).isEmpty();
    }
}
