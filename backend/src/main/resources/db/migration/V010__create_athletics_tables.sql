-- Module Athletics (sprint 4) : état d'adaptation dynamique des athlètes (modèle de Banister).
--
-- athlete_conditions : l'état courant Fitness/Fatigue d'un athlète (aggregate AthleteCondition, clé par
-- athlete_id, ADR-027). État plat (pas de JSONB) : 2 doubles + le timestamp du dernier changement, lu et
-- décroissé à la volée (lazy compute, ADR-006). Une seule paire globale au sprint 4 ; le raffinement par
-- groupe musculaire (sprint 5) enrichira le schéma.
CREATE TABLE athlete_conditions
(
    athlete_id   UUID             NOT NULL PRIMARY KEY,
    fitness      DOUBLE PRECISION NOT NULL,
    fatigue      DOUBLE PRECISION NOT NULL,
    last_updated TIMESTAMPTZ      NOT NULL,
    CONSTRAINT fk_athlete_conditions_athlete FOREIGN KEY (athlete_id) REFERENCES athletes (id) ON DELETE CASCADE
);

-- condition_snapshots : trajectoire append-only, un point capturé à chaque séance appliquée. Alimentera
-- les courbes du sprint 7 (Insights). performance peut être négative (athlète « cuit »). FK CASCADE :
-- si l'athlète disparaît, ses snapshots aussi.
CREATE TABLE condition_snapshots
(
    id          UUID             NOT NULL PRIMARY KEY,
    athlete_id  UUID             NOT NULL,
    taken_at    TIMESTAMPTZ      NOT NULL,
    fitness     DOUBLE PRECISION NOT NULL,
    fatigue     DOUBLE PRECISION NOT NULL,
    performance DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_condition_snapshots_athlete FOREIGN KEY (athlete_id) REFERENCES athletes (id) ON DELETE CASCADE
);

-- Index (athlete_id, taken_at) : sert la lecture chronologique des courbes d'un athlète.
CREATE INDEX idx_condition_snapshots_athlete_taken
    ON condition_snapshots (athlete_id, taken_at);
