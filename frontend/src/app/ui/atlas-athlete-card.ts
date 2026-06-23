import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Rarity } from '../roster/roster.models';
import { AtlasRarityBadge } from './atlas-rarity-badge';

/**
 * Carte d'athlète, variante `summary` (design system §4.10) : pour la grille de l'écurie. Compose
 * Card interactive + RarityBadge. Cliquable → fiche. La variante `detailed` (scout) est composée au
 * niveau page (Card + RarityBadge + StatBlock), conformément à la doctrine « composer les primitives ».
 */
@Component({
  selector: 'atlas-athlete-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, AtlasRarityBadge],
  template: `
    <a
      [routerLink]="['/roster/athletes', id()]"
      class="block rounded-xl bg-[var(--surface-raised)] border border-[var(--border-default)] p-4
             hover:border-[var(--border-strong)] hover:shadow-[var(--shadow-md)]
             focus-visible:outline-none focus-visible:shadow-[var(--focus-ring)]
             transition duration-150"
    >
      <div class="flex items-start justify-between gap-2 mb-2.5">
        <span class="font-display font-semibold text-[1.25rem] leading-tight text-[var(--text-primary)]">{{ name() }}</span>
        <atlas-rarity-badge [rarity]="rarity()" />
      </div>
      <div class="flex items-center gap-2.5 font-mono text-data text-[var(--text-tertiary)]">
        <span>{{ age() }} ans · {{ bodyWeightKg() }} kg</span>
        @if (mirror()) {
          <span class="inline-flex items-center h-[18px] px-[7px] rounded-[5px] bg-[var(--accent-surface)]
                       text-[var(--accent)] text-caption uppercase tracking-[0.06em]">Miroir</span>
        }
      </div>
    </a>
  `,
})
export class AtlasAthleteCard {
  readonly id = input.required<string>();
  readonly name = input.required<string>();
  readonly rarity = input.required<Rarity>();
  readonly mirror = input(false);
  readonly age = input.required<number>();
  readonly bodyWeightKg = input.required<number>();
}
