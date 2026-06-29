-- Sprint 6, Couche 3 (ADR-033) : accumulateur de progression structurelle du 1RM, par pattern de force.
-- Colonne jsonb sur l'aggregate athlete_conditions (le store côté Athletics ; le 1RM matérialisé vit dans
-- roster, ADR-032). Forme : {"byPattern": {"SQUAT": {"startOneRmKg":..., "ceilingOneRmKg":..., "chronicLoad":...}, ...}}.
--
-- DEFAULT '{"byPattern":{}}' : les conditions créées aux sprints 4/5 (avant cette colonne) reçoivent un
-- accumulateur vide — aucune progression entamée, elles se rempliront aux prochaines séances. Backward
-- compatible, pas de backfill calculable (la charge chronique n'a pas d'historique reconstructible).
ALTER TABLE athlete_conditions
    ADD COLUMN structural_progress JSONB NOT NULL DEFAULT '{"byPattern":{}}'::jsonb;
