/** Types du module roster, miroir des DTO backend (voir contrat GATE 2). */

export type Rarity = 'GENERIC' | 'PROMISING' | 'SPECIALIST' | 'PRODIGY';
export type Gender = 'MALE' | 'FEMALE';

export interface GeneticsView {
  hypertrophyPotentialByMuscleGroup: Record<string, number>;
  strengthAffinityByPattern: Record<string, number>;
  baseRecoveryRate: number;
  fiberTypeProfile: number;
  trainingResponseSensitivity: number;
}

export interface Athlete {
  id: string;
  name: string;
  age: number;
  bodyWeightKg: number;
  bodyHeightCm: number;
  gender: Gender;
  rarity: Rarity;
  mirror: boolean;
  genetics: GeneticsView;
  oneRepMaxesKg: Record<string, number>;
  recruitedAt: string;
  trainingHistory: TrainingHistoryView;
}

/** Historique d'entraînement du miroir. `workoutCount` vient de PersonalTraining (option D, ADR-025). */
export interface TrainingHistoryView {
  workoutCount: number;
  lastWorkoutAt: string | null;
  lastPatternsCovered: string[];
}

export type FormState = 'CUIT' | 'FRAIS' | 'AFFUTE';

/**
 * État de forme Banister d'un athlète (module athletics, `/api/athletes/:id/condition`). `fitness`/`fatigue`
 * sont à l'échelle interne NORM (non lisibles en absolu — affichés en proportion). `formIndex` 0–100 et
 * `formState` sont la synthèse de présentation (50 = neutre). `performance` peut être négative (« cuit »).
 */
export interface AthleteCondition {
  athleteId: string;
  fitness: number;
  fatigue: number;
  performance: number;
  formIndex: number;
  formState: FormState;
  asOf: string;
}

export interface AthleteSummary {
  id: string;
  name: string;
  rarity: Rarity;
  mirror: boolean;
  age: number;
  bodyWeightKg: number;
}

export interface Roster {
  id: string;
  hasMirror: boolean;
  athletes: AthleteSummary[];
}

export interface AthleteCandidate {
  name: string;
  age: number;
  bodyWeightKg: number;
  bodyHeightCm: number;
  gender: Gender;
  rarity: Rarity;
  genetics: GeneticsView;
  oneRepMaxesKg: Record<string, number>;
}

export interface ScoutResponse {
  candidateId: string;
  candidate: AthleteCandidate;
}

export interface CreateMirrorRequest {
  name: string;
  age: number;
  bodyWeightKg: number;
  bodyHeightCm: number;
  gender: Gender;
  oneRepMaxes: Record<string, number>;
}
