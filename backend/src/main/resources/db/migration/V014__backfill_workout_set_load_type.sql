-- Sprint 6, couche 1 (ADR-035) — CORRECTIF de V013, qui était inopérante.
--
-- V013 a navigué la colonne `exercises` comme un TABLEAU direct (`jsonb_typeof(exercises) = 'array'`). Or la
-- colonne contient un OBJET `{"exercises": [...]}` (le record `ExercisesJson` enveloppe la liste — jsonb de
-- type object, pas array). V013 a donc matché ZÉRO ligne : elle s'est exécutée (Flyway success=true) sans
-- rien backfiller. Une migration peut « réussir » en ne faisant rien — d'où le GARDE-FOU de complétude
-- ci-dessous, qui aurait fait échouer V013 bruyamment. V013 reste telle quelle (on ne modifie jamais une
-- migration appliquée) ; V014 fait le vrai travail.
--
-- Règle d'inférence (identique au lecteur tolérant du convertisseur et au mapper entrant) :
--   weightKg NULL     -> BODYWEIGHT ;  weightKg non NULL -> EXTERNAL.
-- Les séries qui ONT déjà un loadType (lignes écrites par le nouveau code entre V013 et V014, dont des
-- WEIGHTED) sont PRÉSERVÉES telles quelles — pas de réécriture (idempotent, ne dégrade pas un lesté).

UPDATE workout_sessions ws
SET exercises = jsonb_build_object('exercises', (
    SELECT jsonb_agg(
               ex || jsonb_build_object('sets', (
                   SELECT jsonb_agg(
                              CASE
                                  WHEN st ? 'loadType' THEN st
                                  ELSE st || jsonb_build_object('loadType',
                                      CASE WHEN st ->> 'weightKg' IS NULL THEN 'BODYWEIGHT' ELSE 'EXTERNAL' END)
                              END
                              ORDER BY set_ord)
                   FROM jsonb_array_elements(ex -> 'sets') WITH ORDINALITY AS s(st, set_ord)
               ))
               ORDER BY ex_ord)
    FROM jsonb_array_elements(ws.exercises -> 'exercises') WITH ORDINALITY AS e(ex, ex_ord)
))
WHERE jsonb_typeof(ws.exercises -> 'exercises') = 'array';

-- GARDE-FOU DE COMPLÉTUDE : la migration REFUSE de réussir si une série reste sans loadType.
-- C'est la leçon de V013 : sans cet assert, un backfill silencieusement incomplet passe inaperçu.
DO $$
DECLARE
    missing int;
BEGIN
    SELECT count(*) INTO missing
    FROM workout_sessions ws
    CROSS JOIN LATERAL jsonb_array_elements(ws.exercises -> 'exercises') AS ex
    CROSS JOIN LATERAL jsonb_array_elements(ex -> 'sets') AS st
    WHERE NOT (st ? 'loadType');
    IF missing > 0 THEN
        RAISE EXCEPTION 'V014 : backfill incomplet — % serie(s) sans loadType apres migration', missing;
    END IF;
END $$;
