-- Candidats scoutés, persistés TEMPORAIREMENT (TTL) pour l'intégrité du recrutement (ADR-022).
-- Migration séparée de V005 : concept distinct (temporaire à TTL vs athlètes persistants), ça clarifie
-- la migration. Le candidat complet est un blob jsonb. Index sur expires_at pour la purge périodique.
CREATE TABLE scouted_candidates
(
    id          UUID        NOT NULL PRIMARY KEY,
    candidate   JSONB       NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ
);

CREATE INDEX idx_scouted_candidates_expires_at ON scouted_candidates (expires_at);
