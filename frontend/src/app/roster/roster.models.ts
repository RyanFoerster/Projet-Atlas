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
