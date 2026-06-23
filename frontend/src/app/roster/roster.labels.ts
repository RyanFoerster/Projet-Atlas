import { Rarity } from './roster.models';

/** Libellés FR (voix éditoriale du design system). Le backend garde les enums techniques. */

export const RARITY_LABELS: Record<Rarity, string> = {
  GENERIC: 'Générique',
  PROMISING: 'Prometteur',
  SPECIALIST: 'Spécialiste',
  PRODIGY: 'Phénomène', // pas « Prodige » (jugé enfantin) — registre sport-outlier
};

export const MOVEMENT_LABELS: Record<string, string> = {
  SQUAT: 'Squat',
  BENCH_PRESS: 'Développé couché',
  DEADLIFT: 'Soulevé de terre',
  OVERHEAD_PRESS: 'Développé militaire',
  ROW: 'Tirage',
  CHIN_UP: 'Traction',
};

export const MUSCLE_LABELS: Record<string, string> = {
  CHEST: 'Pectoraux',
  BACK_UPPER: 'Haut du dos',
  BACK_LOWER: 'Bas du dos',
  QUADS: 'Quadriceps',
  HAMSTRINGS: 'Ischios',
  GLUTES: 'Fessiers',
  SHOULDERS: 'Épaules',
  BICEPS: 'Biceps',
  TRICEPS: 'Triceps',
  CALVES: 'Mollets',
  CORE: 'Gainage',
};

/** Ordre d'affichage stable (indépendant de l'ordre des clés JSON). */
export const MOVEMENT_ORDER = ['SQUAT', 'BENCH_PRESS', 'DEADLIFT', 'OVERHEAD_PRESS', 'ROW', 'CHIN_UP'];
export const MUSCLE_ORDER = [
  'CHEST', 'BACK_UPPER', 'BACK_LOWER', 'QUADS', 'HAMSTRINGS', 'GLUTES',
  'SHOULDERS', 'BICEPS', 'TRICEPS', 'CALVES', 'CORE',
];

/** Plages génétiques (alignées sur le domaine) pour normaliser les barres StatBlock. */
export const STRENGTH_RANGE = { min: 0.8, max: 1.25 };
export const HYPERTROPHY_RANGE = { min: 0.85, max: 1.3 };
export const RECOVERY_RANGE = { min: 0.85, max: 1.2 };
export const SENSITIVITY_RANGE = { min: 0.85, max: 1.15 };
export const UNIT_RANGE = { min: 0, max: 1 };
