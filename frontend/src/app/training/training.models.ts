/** Types du module PersonalTraining (frontend). Le backend reste la source de vérité du contrat. */

export type ExerciseCategoryType = 'COMPOUND_FORCE' | 'ACCESSORY';

// ---- Requête : POST /api/personal-training/sessions ----
export interface SetInput {
  reps: number;
  weightKg: number | null; // null = poids de corps
  rpe: number | null; // null = non renseigné
}

export interface ExerciseInput {
  name: string;
  pattern: string | null; // MovementPattern (si composé)
  region: string | null; // BodyRegion (si accessoire)
  sets: SetInput[];
}

export interface LogWorkoutRequest {
  performedAt: string; // ISO 8601
  durationMinutes: number | null;
  notes: string | null;
  exercises: ExerciseInput[];
}

// ---- Réponse détail : WorkoutSessionDto ----
export interface SetDetail {
  reps: number;
  weightKg: number | null;
  rpe: number | null;
}

export interface ExerciseDetail {
  name: string;
  category: ExerciseCategoryType;
  pattern: string | null;
  region: string | null;
  sets: SetDetail[];
}

export interface WorkoutSession {
  id: string;
  performedAt: string;
  durationMinutes: number | null;
  notes: string | null;
  totalSets: number;
  totalReps: number;
  estimatedVolumeKg: number;
  patternsCovered: string[];
  exercises: ExerciseDetail[];
  createdAt: string;
}

// ---- Réponse historique paginé : WorkoutHistoryDto ----
export interface WorkoutHistoryItem {
  id: string;
  performedAt: string;
  durationMinutes: number | null;
  exerciseCount: number;
  totalSets: number;
  totalReps: number;
  patternsCovered: string[];
}

export interface WorkoutHistory {
  sessions: WorkoutHistoryItem[];
  page: number;
  size: number;
  total: number;
}
