import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ThemeToggle } from '../theme/theme-toggle';
import { AtlasIcon } from '../ui/atlas-icon';

/**
 * Châssis de l'app connectée (top bar + nav + contenu). Réutilisé par les écrans roster ET training :
 * la nav (Écurie · Entraînement) est accessible depuis n'importe où. Le layout App complet avec sidebar
 * (design system §5.1) viendra quand il y aura plus de sections ; avec deux sections, la top bar suffit.
 */
@Component({
  selector: 'atlas-roster-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, RouterLinkActive, ThemeToggle, AtlasIcon],
  template: `
    <div class="min-h-screen bg-[var(--surface-base)] text-[var(--text-primary)]">
      <header
        class="sticky top-0 z-20 flex items-center justify-between h-14 px-6 md:px-8
               bg-[var(--surface-base)] border-b border-[var(--border-subtle)]"
      >
        <div class="flex items-center gap-6">
          <a routerLink="/roster" class="font-display font-semibold text-[1.3rem] uppercase tracking-[0.22em] text-[var(--text-primary)]">
            Atlas
          </a>
          <nav class="flex items-center gap-1">
            <a
              routerLink="/roster"
              routerLinkActive="bg-[var(--accent-surface)] !text-[var(--text-primary)]"
              class="flex items-center gap-2 h-9 px-3 rounded-lg font-sans text-body-sm text-[var(--text-secondary)]
                     hover:text-[var(--text-primary)] transition-colors duration-100"
            >
              <atlas-icon name="search" [size]="16" />Écurie
            </a>
            <a
              routerLink="/training"
              routerLinkActive="bg-[var(--accent-surface)] !text-[var(--text-primary)]"
              class="flex items-center gap-2 h-9 px-3 rounded-lg font-sans text-body-sm text-[var(--text-secondary)]
                     hover:text-[var(--text-primary)] transition-colors duration-100"
            >
              <atlas-icon name="activity" [size]="16" />Entraînement
            </a>
          </nav>
        </div>
        <atlas-theme-toggle />
      </header>
      <main class="max-w-[1120px] mx-auto px-6 md:px-8 py-10">
        <ng-content />
      </main>
    </div>
  `,
})
export class RosterShell {}
