-- Table des liens magiques (entity MagicLink du module identity).
-- token = UUID v7 = clé primaire (secret porté dans l'URL).
-- On stocke user_email (pas user_id) : au moment de l'émission, le Player peut ne pas exister
-- encore (premier login = signup implicite) — cf. logique métier du flow magic link.
CREATE TABLE magic_links
(
    token       UUID         NOT NULL PRIMARY KEY,
    user_email  VARCHAR(254) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    expires_at  TIMESTAMPTZ  NOT NULL,
    consumed_at TIMESTAMPTZ,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(512)
);

-- Index sur user_email : retrouver les liens d'un email (rate limiting futur, audit).
CREATE INDEX idx_magic_links_user_email ON magic_links (user_email);
-- Index sur expires_at : nettoyage par lots des liens expirés (cleanup job futur).
CREATE INDEX idx_magic_links_expires_at ON magic_links (expires_at);
