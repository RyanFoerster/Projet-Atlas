-- Module Athletics (sprint 5, Couche 3) : individualisation génétique du modèle de Banister (ADR-031).
-- On dénormalise les modificateurs génétiques dérivés sur athlete_conditions — la Genetics étant IMMUTABLE,
-- ils sont résolus une seule fois (à la création de la condition) et relus à chaque lecture (lazy compute),
-- sans rappeler Roster. Zéro divergence possible (immutable).
--
--   recovery_rate        : baseRecoveryRate (0.85–1.20) → module τ_fatigue = τ_fatigue / recovery_rate.
--   stimulus_multiplier  : trainingResponseSensitivity (0.85–1.15) → module la magnitude du stimulus.
--
-- DEFAULT 1.0 = neutre (aucun effet génétique) : couvre d'éventuelles lignes créées avant cette migration ;
-- les nouvelles conditions écrivent toujours les valeurs explicites.
ALTER TABLE athlete_conditions
    ADD COLUMN recovery_rate       DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    ADD COLUMN stimulus_multiplier DOUBLE PRECISION NOT NULL DEFAULT 1.0;
