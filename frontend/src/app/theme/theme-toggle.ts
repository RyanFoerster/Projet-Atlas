import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ThemeService } from './theme.service';

/**
 * Bouton de bascule de thème dark/light.
 *
 * Accessibilité : c'est un vrai <button> (clavier natif : Entrée/Espace), avec un
 * `aria-label` qui décrit l'action, `aria-pressed` qui reflète l'état dark, et un
 * focus ring bronze visible (jamais d'outline retirée sans remplacement).
 *
 * Icônes : SVG Lucide inline (sun / moon). `lucide-angular` ne supporte pas encore
 * Angular 22 (cf. ADR-016) ; on inline les 2 icônes plutôt que de forcer une dépendance
 * incompatible. Stroke 1.75, `currentColor` — conforme au design system §7.
 */
@Component({
  selector: 'atlas-theme-toggle',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      type="button"
      class="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-[var(--border-default)]
             bg-transparent text-[var(--text-secondary)]
             transition-colors duration-150
             hover:bg-[var(--accent-surface)] hover:text-[var(--text-primary)]
             focus-visible:outline-none focus-visible:shadow-[var(--focus-ring)]"
      [attr.aria-label]="label()"
      [attr.aria-pressed]="theme.isDark()"
      (click)="theme.toggle()"
    >
      @if (theme.isDark()) {
        <!-- sun : en dark, le clic mène au thème clair -->
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.75"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <circle cx="12" cy="12" r="4" />
          <path d="M12 2v2" />
          <path d="M12 20v2" />
          <path d="m4.93 4.93 1.41 1.41" />
          <path d="m17.66 17.66 1.41 1.41" />
          <path d="M2 12h2" />
          <path d="M20 12h2" />
          <path d="m6.34 17.66-1.41 1.41" />
          <path d="m19.07 4.93-1.41 1.41" />
        </svg>
      } @else {
        <!-- moon : en light, le clic mène au thème sombre -->
        <svg
          width="20"
          height="20"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.75"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <path d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z" />
        </svg>
      }
    </button>
  `,
})
export class ThemeToggle {
  protected readonly theme = inject(ThemeService);

  protected readonly label = computed(() =>
    this.theme.isDark() ? 'Passer en thème clair' : 'Passer en thème sombre',
  );
}
