-- Module Athletics (sprint 5) : la forme passe d'une paire globale à une forme PAR GROUPE MUSCULAIRE
-- (ADR-004 réalisé, ADR-029). athlete_conditions.by_muscle stocke désormais une Map<MuscleGroup,
-- {fitness, fatigue}> en jsonb (pattern sprint 2/3/4), lue et décroissée à la volée (lazy compute, ADR-006).
--
-- RESET ASSUMÉ (pré-beta) : les conditions du sprint 4 sont une paire GLOBALE dont on ne connaît pas la
-- répartition par muscle (le modèle global ne la stockait pas). Un backfill devrait INVENTER une
-- distribution = données fausses. On préfère une condition vide honnête qui repart proprement à la
-- prochaine séance. Les lignes globales sont donc supprimées (incompatibilité structurelle assumée) ; aucune
-- ancienne ligne ne doit être lue par le nouveau code (désérialisation impossible). Neutre : seul user,
-- déjà reset pendant le debug sprint 4. Cohérent avec la note « backfill » de la rétro sprint 4.
DELETE FROM athlete_conditions;

ALTER TABLE athlete_conditions
    DROP COLUMN fitness,
    DROP COLUMN fatigue;

ALTER TABLE athlete_conditions
    ADD COLUMN by_muscle JSONB NOT NULL;

-- condition_snapshots reste inchangée : un snapshot est AGRÉGÉ (fitness/fatigue sommées sur les muscles —
-- arbitrage sprint 5), structurellement identique au sprint 4. La trajectoire append-only continue sans
-- migration.
