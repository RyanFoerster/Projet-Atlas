/*
 * Icônes Lucide (https://lucide.dev) — ISC/MIT License, Copyright (c) Lucide Contributors.
 * Les tracés ci-dessous sont copiés tels quels depuis lucide.dev (aucune interprétation).
 */
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/** Icônes enregistrées. Union stricte : TypeScript refuse toute icône non listée à la compilation. */
export type AtlasIconName =
  | 'mail' | 'alert-circle' | 'arrow-right' | 'arrow-left' | 'log-out' | 'sun' | 'moon' | 'search'
  | 'plus' | 'x' | 'trash-2' | 'chevron-down' | 'activity' | 'trending-up';

/** Tailles canoniques du design system (§7). Pas de taille libre. */
export type AtlasIconSize = 16 | 20 | 24;

/**
 * Icône fonctionnelle du design system (Lucide uniquement, §7). Couleur héritée via
 * {@code currentColor} (pas de prop color) → intégration native avec les tokens de texte.
 *
 * Usage : {@code <atlas-icon name="mail" [size]="20" />}. Le {@code name} est typé : une icône
 * non enregistrée ou une typo casse la compilation. Les nouvelles icônes s'ajoutent ici, en
 * copiant le tracé depuis lucide.dev et en étendant {@link AtlasIconName}.
 */
@Component({
  selector: 'atlas-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <svg
      [attr.width]="size()"
      [attr.height]="size()"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.75"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      @switch (name()) {
        @case ('mail') {
          <rect width="20" height="16" x="2" y="4" rx="2" />
          <path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7" />
        }
        @case ('alert-circle') {
          <circle cx="12" cy="12" r="10" />
          <line x1="12" x2="12" y1="8" y2="12" />
          <line x1="12" x2="12.01" y1="16" y2="16" />
        }
        @case ('arrow-right') {
          <path d="M5 12h14" />
          <path d="m12 5 7 7-7 7" />
        }
        @case ('arrow-left') {
          <path d="m12 19-7-7 7-7" />
          <path d="M19 12H5" />
        }
        @case ('search') {
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.3-4.3" />
        }
        @case ('log-out') {
          <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
          <polyline points="16 17 21 12 16 7" />
          <line x1="21" x2="9" y1="12" y2="12" />
        }
        @case ('sun') {
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2" />
          <path d="M12 20v2" />
          <path d="m4.93 4.93 1.41 1.41" />
          <path d="m17.66 17.66 1.41 1.41" />
          <path d="M2 12h2" />
          <path d="M20 12h2" />
          <path d="m6.34 17.66-1.41 1.41" />
          <path d="m19.07 4.93-1.41 1.41" />
        }
        @case ('moon') {
          <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z" />
        }
        @case ('plus') {
          <path d="M5 12h14" />
          <path d="M12 5v14" />
        }
        @case ('x') {
          <path d="M18 6 6 18" />
          <path d="m6 6 12 12" />
        }
        @case ('trash-2') {
          <path d="M3 6h18" />
          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />
          <path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
          <line x1="10" x2="10" y1="11" y2="17" />
          <line x1="14" x2="14" y1="11" y2="17" />
        }
        @case ('chevron-down') {
          <path d="m6 9 6 6 6-6" />
        }
        @case ('activity') {
          <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
        }
        @case ('trending-up') {
          <path d="M16 7h6v6" />
          <path d="m22 7-8.5 8.5-5-5L2 17" />
        }
      }
    </svg>
  `,
})
export class AtlasIcon {
  readonly name = input.required<AtlasIconName>();
  readonly size = input<AtlasIconSize>(20);
}
