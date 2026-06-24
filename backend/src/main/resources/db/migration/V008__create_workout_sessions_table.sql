-- Séances d'entraînement IRL loggées par le Player (module personaltraining, sprint 3).
-- Chaque séance est un aggregate autonome. Les exercices (structure imbriquée exercice → séries, avec
-- catégorie sealed aplatie en discriminant) sont stockés en JSONB : pas de query SQL fine dessus au
-- sprint 3, cohérent avec le pattern Roster (genetics/current_stats, ADR-019).
CREATE TABLE workout_sessions
(
    id               UUID         NOT NULL PRIMARY KEY,
    owner_id         UUID         NOT NULL,
    performed_at     TIMESTAMPTZ  NOT NULL,
    duration_minutes INT,                       -- nullable : non renseigné
    notes            VARCHAR(500),              -- nullable : pas de notes
    exercises        JSONB        NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_workout_sessions_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Index composite (owner_id, performed_at DESC) : sert l'historique d'un Player trié du plus récent au
-- plus ancien ET les recherches par propriétaire (préfixe gauche). Il subsume un index seul sur owner_id.
CREATE INDEX idx_workout_sessions_owner_performed
    ON workout_sessions (owner_id, performed_at DESC);
