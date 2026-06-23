import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ThemeService } from './theme.service';
import { AtlasIcon } from '../ui/atlas-icon';

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
  imports: [AtlasIcon],
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
      <!-- en dark → sun (clic = thème clair) ; en light → moon (clic = thème sombre) -->
      <atlas-icon [name]="theme.isDark() ? 'sun' : 'moon'" [size]="20" />
    </button>
  `,
})
export class ThemeToggle {
  protected readonly theme = inject(ThemeService);

  protected readonly label = computed(() =>
    this.theme.isDark() ? 'Passer en thème clair' : 'Passer en thème sombre',
  );
}
