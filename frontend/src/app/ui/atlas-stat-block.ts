import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

/**
 * Barre de valeur normalisée (design system §4.9). Affiche une valeur génétique dans sa plage, avec
 * un repère de baseline à 1.0 (la « moyenne »). Fill bronze neutre — jamais de vert/rouge sémantique
 * (réservé aux deltas dynamiques). Au-dessus de 1.15, l'axe est mis en évidence (point + semibold).
 */
@Component({
  selector: 'atlas-stat-block',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex items-center gap-4">
      <div
        class="w-[140px] shrink-0 font-sans text-caption uppercase tracking-[0.06em] truncate"
        [style.color]="strong() ? 'var(--text-primary)' : 'var(--text-tertiary)'"
        [style.fontWeight]="strong() ? '600' : '500'"
      >{{ label() }}</div>

      <div class="relative flex-1 h-2 rounded-[4px] bg-[var(--surface-sunken)] border border-[var(--border-subtle)]">
        <div
          class="absolute left-0 top-0 bottom-0 rounded-[4px] bg-[var(--accent)]"
          [style.width.%]="fillPercent()"
          [style.opacity]="strong() ? '1' : '0.8'"
        ></div>
        @if (showBaseline()) {
          <div
            class="absolute -top-[3px] -bottom-[3px] w-[2px] bg-[var(--text-tertiary)]"
            [style.left.%]="baselinePercent()"
            aria-hidden="true"
          ></div>
        }
      </div>

      <div
        class="w-[46px] shrink-0 text-right font-mono text-data flex items-center justify-end gap-[5px]"
        [style.color]="strong() ? 'var(--text-primary)' : 'var(--text-secondary)'"
        [style.fontWeight]="strong() ? '600' : '400'"
      >
        @if (strong()) {
          <span class="w-[5px] h-[5px] rounded-full bg-[var(--accent)]" aria-hidden="true"></span>
        }
        {{ value().toFixed(2) }}
      </div>
    </div>
  `,
})
export class AtlasStatBlock {
  readonly label = input.required<string>();
  readonly value = input.required<number>();
  readonly min = input(0.8);
  readonly max = input(1.25);

  protected readonly fillPercent = computed(() => {
    const ratio = (this.value() - this.min()) / (this.max() - this.min());
    return Math.max(0, Math.min(100, ratio * 100));
  });
  protected readonly baselinePercent = computed(() => ((1 - this.min()) / (this.max() - this.min())) * 100);
  protected readonly showBaseline = computed(() => this.min() < 1 && this.max() > 1);
  protected readonly strong = computed(() => this.value() >= 1.15);
}
