import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { GeneticsView } from './roster.models';
import {
  HYPERTROPHY_RANGE, MOVEMENT_LABELS, MOVEMENT_ORDER, MUSCLE_LABELS, MUSCLE_ORDER,
  RECOVERY_RANGE, SENSITIVITY_RANGE, STRENGTH_RANGE, UNIT_RANGE,
} from './roster.labels';
import { AtlasStatBlock } from '../ui/atlas-stat-block';

interface Row {
  label: string;
  value: number;
  min: number;
  max: number;
}

/**
 * Affiche la génétique complète d'un athlète/candidat via des StatBlock regroupés (Force,
 * Hypertrophie, Autres). Toujours TOUS les axes (un profil incomplet masquerait les points faibles
 * d'un Phénomène déséquilibré — design system §4.10).
 */
@Component({
  selector: 'atlas-genetics-panel',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AtlasStatBlock],
  template: `
    @for (group of groups(); track group.title) {
      <div class="mb-5 last:mb-0">
        <p class="mb-3 font-sans text-caption uppercase tracking-[0.08em] text-[var(--text-tertiary)]">{{ group.title }}</p>
        <div class="flex flex-col gap-2.5">
          @for (row of group.rows; track row.label) {
            <atlas-stat-block [label]="row.label" [value]="row.value" [min]="row.min" [max]="row.max" />
          }
        </div>
      </div>
    }
  `,
})
export class AtlasGeneticsPanel {
  readonly genetics = input.required<GeneticsView>();

  protected readonly groups = computed<{ title: string; rows: Row[] }[]>(() => {
    const g = this.genetics();
    const strength: Row[] = MOVEMENT_ORDER.map((k) => ({
      label: MOVEMENT_LABELS[k], value: g.strengthAffinityByPattern[k], ...STRENGTH_RANGE,
    }));
    const hypertrophy: Row[] = MUSCLE_ORDER.map((k) => ({
      label: MUSCLE_LABELS[k], value: g.hypertrophyPotentialByMuscleGroup[k], ...HYPERTROPHY_RANGE,
    }));
    const others: Row[] = [
      { label: 'Récupération', value: g.baseRecoveryRate, ...RECOVERY_RANGE },
      { label: 'Type de fibres', value: g.fiberTypeProfile, ...UNIT_RANGE },
      { label: 'Sensibilité', value: g.trainingResponseSensitivity, ...SENSITIVITY_RANGE },
    ];
    return [
      { title: 'Force — par mouvement', rows: strength },
      { title: 'Hypertrophie — par groupe', rows: hypertrophy },
      { title: 'Autres', rows: others },
    ];
  });
}
