package dev.ryanfoerster.atlas.identity.infrastructure.email;

import dev.ryanfoerster.atlas.identity.domain.model.MagicLinkToken;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplatesTest {

    private final EmailTemplates templates = new EmailTemplates("http://localhost:4200");

    @Test
    void builds_the_callback_url_with_the_token() {
        MagicLinkToken token = MagicLinkToken.generate();

        assertThat(templates.magicLinkUrl(token))
                .isEqualTo("http://localhost:4200/auth/callback?token=" + token.value());
    }

    @Test
    void magic_link_message_contains_the_link_and_a_subject() {
        MagicLinkToken token = MagicLinkToken.generate();

        EmailTemplates.Message message = templates.magicLink(token);

        assertThat(message.subject()).isEqualTo("Ton lien de connexion à Atlas");
        assertThat(message.body()).contains(templates.magicLinkUrl(token));
        assertThat(message.body()).contains("15 minutes");
    }
}
