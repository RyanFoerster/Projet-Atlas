import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { AthleteCondition, FormState } from '../roster/roster.models';

interface StateStyle {
  label: string;
  color: string;
  pillBg: string;
  pillBorder: string;
}

// Couleurs minérales, jamais de feu tricolore (design system §4.16). « Cuit » = ambre (--warning), PAS
// --danger : la sur-fatigue est un état d'entraînement normal, pas une erreur.
const STATES: Record<FormState, StateStyle> = {
  AFFUTE: { label: 'Affûté', color: 'var(--success)', pillBg: 'var(--success-surface)', pillBorder: 'none' },
  FRAIS: { label: 'Frais', color: 'var(--text-secondary)', pillBg: 'var(--surface-raised-2)', pillBorder: '1px solid var(--border-default)' },
  CUIT: { label: 'Cuit', color: 'var(--warning)', pillBg: 'var(--warning-surface)', pillBorder: 'none' },
};

/**
 * Indicateur de Forme — état Fitness-Fatigue de Banister (design system §4.16). Dynamique (≠ StatBlock §4.9
 * statique), donc couleurs sémantiques légitimes. Indice 0–100 + état, grande barre (tick neutre à 50),
 * et détail Acquis/Fatigue en proportion relative (l'échelle interne NORM n'est pas lisible en absolu).
 */
@Component({
  selector: 'atlas-condition-gauge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      class="rounded-xl bg-[var(--surface-base)] border border-[var(--border-subtle)] p-5"
      role="img"
      [attr.aria-label]="ariaLabel()"
    >
      <div class="flex items-end justify-between gap-3 mb-3">
        <div>
          <div class="mb-1 font-sans text-caption uppercase tracking-[0.08em] text-[var(--text-tertiary)]">Forme</div>
          <div class="font-mono tabular-nums text-data-lg leading-none text-[var(--text-primary)]">
            {{ index() }}<span class="text-[1.05rem] text-[var(--text-tertiary)]">/100</span>
          </div>
        </div>
        <span
          class="inline-flex items-center h-[22px] px-[9px] rounded-md font-sans uppercase tracking-[0.08em] text-caption whitespace-nowrap"
          [style.background]="state().pillBg"
          [style.color]="state().color"
          [style.border]="state().pillBorder"
        >{{ state().label }}</span>
      </div>

      <!-- Grande barre de forme 0–100, tick neutre à 50, fill = couleur d'état -->
      <div class="relative h-2 rounded-[4px] bg-[var(--surface-sunken)] border border-[var(--border-subtle)]">
        <div
          class="absolute left-0 top-0 bottom-0 rounded-[4px]"
          [style.width.%]="index()"
          [style.background]="state().color"
        ></div>
        <div class="absolute -top-[3px] -bottom-[3px] left-1/2 w-[2px] bg-[var(--text-tertiary)]" aria-hidden="true"></div>
      </div>

      <!-- Détail Acquis / Fatigue : mini-barres plus fines (visuellement distinctes de la grande) -->
      <div class="mt-4 flex flex-col gap-2.5">
        <div class="flex items-center gap-3">
          <span class="w-[58px] shrink-0 font-sans text-[0.6875rem] uppercase tracking-[0.06em] text-[var(--text-tertiary)]">Acquis</span>
          <div class="relative flex-1 h-1 rounded-full bg-[var(--surface-sunken)]">
            <div class="absolute left-0 top-0 bottom-0 rounded-full bg-[var(--accent)]" [style.width.%]="acquisPercent()"></div>
          </div>
        </div>
        <div class="flex items-center gap-3">
          <span class="w-[58px] shrink-0 font-sans text-[0.6875rem] uppercase tracking-[0.06em] text-[var(--text-tertiary)]">Fatigue</span>
          <div class="relative flex-1 h-1 rounded-full bg-[var(--surface-sunken)]">
            <div class="absolute left-0 top-0 bottom-0 rounded-full bg-[var(--warning)]" [style.width.%]="fatiguePercent()"></div>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class AtlasConditionGauge {
  readonly condition = input.required<AthleteCondition>();

  protected readonly state = computed(() => STATES[this.condition().formState]);
  protected readonly index = computed(() => Math.max(0, Math.min(100, Math.round(this.condition().formIndex))));

  // Mini-barres en proportion relative : normalisées sur le max(fitness, fatigue) — on lit le rapport
  // acquis/fatigue sans prétendre à une unité (l'échelle NORM est interne).
  private readonly peak = computed(() => Math.max(this.condition().fitness, this.condition().fatigue, 1e-9));
  protected readonly acquisPercent = computed(() => Math.min(100, (this.condition().fitness / this.peak()) * 100));
  protected readonly fatiguePercent = computed(() => Math.min(100, (this.condition().fatigue / this.peak()) * 100));

  protected readonly ariaLabel = computed(
    () => `Forme : ${this.index()} sur 100, ${this.state().label.toLowerCase()}`,
  );
}
