-- Athlètes (entities internes de l'aggregate Roster). genetics/current_stats en JSONB (structures
-- complexes, pas de query SQL dessus au sprint 2 — ADR-019). Un seul miroir par roster, garanti par
-- un index unique partiel (défense en profondeur, en plus de l'invariant domaine).
CREATE TABLE athletes
(
    id             UUID         NOT NULL PRIMARY KEY,
    roster_id      UUID         NOT NULL,
    name           VARCHAR(50)  NOT NULL,
    age            INT          NOT NULL,
    body_weight_kg NUMERIC      NOT NULL,
    body_height_cm INT          NOT NULL,
    gender         VARCHAR(10)  NOT NULL,
    rarity         VARCHAR(20)  NOT NULL,
    is_mirror      BOOLEAN      NOT NULL,
    genetics       JSONB        NOT NULL,
    current_stats  JSONB        NOT NULL,
    recruited_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_athletes_roster FOREIGN KEY (roster_id) REFERENCES rosters (id) ON DELETE CASCADE
);

CREATE INDEX idx_athletes_roster_id ON athletes (roster_id);
-- Au plus un athlète miroir par roster.
CREATE UNIQUE INDEX uq_athletes_one_mirror ON athletes (roster_id) WHERE is_mirror = TRUE;
