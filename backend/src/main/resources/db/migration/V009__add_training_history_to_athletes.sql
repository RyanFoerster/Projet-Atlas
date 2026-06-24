-- TrainingHistory de l'athlète miroir (sprint 3, ADR-025) : trace passive de la dernière séance IRL
-- reçue (date + patterns de force couverts). Le NOMBRE de séances n'est PAS stocké ici — sa source de
-- vérité est PersonalTraining (option D : pas de duplication du compteur → pas de problème d'idempotence
-- sur le count ; cf. ADR-025). Stocké en JSONB, cohérent avec genetics/current_stats (ADR-019).
--
-- Colonne NOT NULL avec un défaut « historique vierge » : tout athlète (miroir comme virtuel) a un
-- TrainingHistory, vide tant qu'aucune séance n'a été reçue. La forme du défaut matche le DTO
-- TrainingHistoryJson (lastWorkoutAt nullable, lastPatternsCovered liste).
ALTER TABLE athletes
    ADD COLUMN training_history JSONB NOT NULL
        DEFAULT '{"lastWorkoutAt": null, "lastPatternsCovered": []}'::jsonb;
