-- Sprint 6, couche 1 (ADR-035) : saisie de charge typée (poids de corps / lesté / externe).
--
-- Chaque série passe d'une charge implicite (weightKg null = poids de corps) à un discriminant explicite
-- `loadType`. On BACKFILL les séances existantes au lieu de les réinitialiser : `workout_sessions` est une
-- donnée IRL SOURCE (les vraies séances du joueur, le hook de l'app), non recalculable — contrairement aux
-- `athlete_conditions` dérivées qu'on a pu reset au sprint 5.
--
-- Interprétation historique (seule défendable automatiquement) :
--   weightKg NULL      -> BODYWEIGHT (poids de corps)
--   weightKg non NULL  -> EXTERNAL   (charge externe ; le type LESTÉ/WEIGHTED est une capacité nouvelle,
--                                     aucune donnée historique ne peut être qualifiée de lestée)
--
-- On ajoute le champ `loadType` à chaque série du JSONB `exercises`, en préservant l'ordre des tableaux
-- (WITH ORDINALITY + ORDER BY) et sans toucher aux autres champs (reps, weightKg, rpe) ni à l'exercice
-- (name, categoryType, pattern, region) grâce à l'opérateur de fusion `||`.

UPDATE workout_sessions ws
SET exercises = (
    SELECT jsonb_agg(
               ex || jsonb_build_object('sets', (
                   SELECT jsonb_agg(
                              st || jsonb_build_object(
                                  'loadType',
                                  CASE WHEN st ->> 'weightKg' IS NULL THEN 'BODYWEIGHT' ELSE 'EXTERNAL' END)
                              ORDER BY set_ord)
                   FROM jsonb_array_elements(ex -> 'sets') WITH ORDINALITY AS s(st, set_ord)
               ))
               ORDER BY ex_ord)
    FROM jsonb_array_elements(ws.exercises) WITH ORDINALITY AS e(ex, ex_ord)
)
WHERE jsonb_typeof(exercises) = 'array';
