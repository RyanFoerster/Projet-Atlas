import { MOVEMENT_LABELS, MOVEMENT_ORDER } from '../roster/roster.labels';
import { SelectOption } from '../ui/atlas-select';

/** Libellés FR du module training (voix éditoriale du design system). Backend = enums techniques. */

/** Régions accessoires (BodyRegion). « Gainage » est le terme FR d'entraînement pour CORE. */
export const BODY_REGION_LABELS: Record<string, string> = {
  CHEST: 'Pectoraux',
  BACK: 'Dos',
  SHOULDERS: 'Épaules',
  BICEPS: 'Biceps',
  TRICEPS: 'Triceps',
  FOREARMS: 'Avant-bras',
  CORE: 'Gainage',
  GLUTES: 'Fessiers',
  QUADS: 'Quadriceps',
  HAMSTRINGS: 'Ischios',
  CALVES: 'Mollets',
};

export const BODY_REGION_ORDER = [
  'CHEST', 'BACK', 'SHOULDERS', 'BICEPS', 'TRICEPS', 'FOREARMS',
  'CORE', 'GLUTES', 'QUADS', 'HAMSTRINGS', 'CALVES',
];

/** Options pour les `atlas-select` du logger. */
export const MOVEMENT_OPTIONS: SelectOption[] = MOVEMENT_ORDER.map((value) => ({
  value,
  label: MOVEMENT_LABELS[value] ?? value,
}));

export const BODY_REGION_OPTIONS: SelectOption[] = BODY_REGION_ORDER.map((value) => ({
  value,
  label: BODY_REGION_LABELS[value] ?? value,
}));

export function movementLabel(pattern: string | null): string {
  return pattern ? MOVEMENT_LABELS[pattern] ?? pattern : '';
}

export function bodyRegionLabel(region: string | null): string {
  return region ? BODY_REGION_LABELS[region] ?? region : '';
}

/**
 * Date relative en français (« aujourd'hui », « hier », « il y a 3 jours »), au-delà d'une semaine la
 * date absolue courte. Calculée par différence de jours calendaires.
 */
export function relativeDate(iso: string, now: Date = new Date()): string {
  const then = new Date(iso);
  const startOfDay = (d: Date) => Date.UTC(d.getFullYear(), d.getMonth(), d.getDate());
  const days = Math.round((startOfDay(now) - startOfDay(then)) / 86_400_000);
  if (days <= 0) return "aujourd'hui";
  if (days === 1) return 'hier';
  if (days < 7) return `il y a ${days} jours`;
  if (days < 14) return 'il y a une semaine';
  return then.toLocaleDateString('fr-BE', { day: 'numeric', month: 'short', year: 'numeric' });
}

/** Date+heure absolue courte (mono), ex. « 23 juin, 18:30 ». */
export function absoluteDateTime(iso: string): string {
  return new Date(iso).toLocaleString('fr-BE', {
    day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit',
  });
}
