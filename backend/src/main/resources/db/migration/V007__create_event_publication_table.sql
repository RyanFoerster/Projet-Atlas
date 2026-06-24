-- ==========================================================================
-- Event Publication Registry de Spring Modulith (sprint 3, ADR-023)
--
-- Table interne de Modulith qui rend la communication inter-module DURABLE :
-- chaque event applicatif (ex. WorkoutLogged) est persisté ici AVANT que son
-- handler s'exécute, puis la ligne est marquée complète (completion_date) au
-- succès. Si le handler échoue, la publication reste incomplète et durable —
-- re-livrée au démarrage de l'app (republish-outstanding-events-on-restart).
--
-- DDL repris VERBATIM du schéma officiel Modulith 2.1.0 pour PostgreSQL
-- (spring-modulith-events-jdbc, schemas/v2/schema-postgresql.sql). L'entité JPA
-- JpaEventPublication est validée contre cette table au boot (ddl-auto: validate),
-- d'où la nécessité de respecter exactement ces colonnes/types.
-- Les colonnes status / completion_attempts / last_resubmission_date sont la
-- nouveauté du schéma v2 (support de la resoumission des publications incomplètes).
-- ==========================================================================
CREATE TABLE IF NOT EXISTS event_publication
(
  id                     UUID NOT NULL,
  listener_id            TEXT NOT NULL,
  event_type             TEXT NOT NULL,
  serialized_event       TEXT NOT NULL,
  publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
  completion_date        TIMESTAMP WITH TIME ZONE,
  status                 TEXT,
  completion_attempts    INT,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS event_publication_serialized_event_hash_idx
  ON event_publication USING hash (serialized_event);

CREATE INDEX IF NOT EXISTS event_publication_by_completion_date_idx
  ON event_publication (completion_date);
