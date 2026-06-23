-- Écurie d'un Player (aggregate Roster du module roster). owner_id unique : un Player a au plus un roster.
-- FK vers users : intégrité référentielle au niveau base (le DB partage les tables ; à garder en tête
-- si un module est un jour extrait — ADR-001/ADR-019).
CREATE TABLE rosters
(
    id         UUID        NOT NULL PRIMARY KEY,
    owner_id   UUID        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_rosters_owner_id UNIQUE (owner_id),
    CONSTRAINT fk_rosters_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);
