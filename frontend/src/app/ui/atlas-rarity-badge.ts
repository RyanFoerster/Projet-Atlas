import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { Rarity } from '../roster/roster.models';
import { RARITY_LABELS } from '../roster/roster.labels';

interface BadgeStyle {
  bg: string;
  color: string;
  border: string;
  weight: string;
}

// Variante A (design system §4.8) : 3 paliers en surface teintée sobre, Phénomène en aplat plein.
// Hiérarchie par graisse + saturation, jamais par saut de teinte. Couleurs en style inline (le
// color-mix de l'ocre passe mieux ainsi qu'en classe Tailwind arbitraire).
const STYLES: Record<Rarity, BadgeStyle> = {
  GENERIC: { bg: 'var(--surface-raised-2)', color: 'var(--text-tertiary)', border: '1px solid var(--border-default)', weight: '500' },
  PROMISING: { bg: 'color-mix(in srgb, var(--dv-7) 16%, transparent)', color: 'var(--dv-7)', border: 'none', weight: '500' },
  SPECIALIST: { bg: 'var(--accent-surface)', color: 'var(--accent)', border: 'none', weight: '500' },
  PRODIGY: { bg: 'var(--accent)', color: 'var(--accent-text-on)', border: 'none', weight: '600' }, // contraste 7.6:1 (AAA)
};

/** Badge de rareté d'un athlète (design system §4.8). Variante de Badge/Tag. */
@Component({
  selector: 'atlas-rarity-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <span
      class="inline-flex items-center h-[22px] px-[9px] rounded-md font-sans uppercase tracking-[0.08em] text-caption whitespace-nowrap"
      [style.background]="style().bg"
      [style.color]="style().color"
      [style.border]="style().border"
      [style.fontWeight]="style().weight"
    >{{ label() }}</span>
  `,
})
export class AtlasRarityBadge {
  readonly rarity = input.required<Rarity>();

  protected readonly style = computed(() => STYLES[this.rarity()]);
  protected readonly label = computed(() => RARITY_LABELS[this.rarity()]);
}
