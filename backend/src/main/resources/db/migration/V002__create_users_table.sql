-- Table des Players (aggregate User du module identity).
-- Clé primaire UUID v7 générée côté application (ADR-008, ADR-014) : ordonnée dans le temps,
-- donc index quasi-séquentiel. Email unique = invariant métier contrôlé aussi en base.
CREATE TABLE users
(
    id            UUID         NOT NULL PRIMARY KEY,
    email         VARCHAR(254) NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    locale        VARCHAR(35)  NOT NULL,
    timezone      VARCHAR(64)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    last_login_at TIMESTAMPTZ,
    CONSTRAINT uq_users_email UNIQUE (email)
);
