import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ThemeToggle } from '../theme/theme-toggle';

/**
 * Châssis léger des écrans roster (top bar + contenu contenu). Le layout App complet avec sidebar
 * (design system §5.1) viendra quand il y aura plusieurs sections ; au sprint 2, une top bar suffit.
 */
@Component({
  selector: 'atlas-roster-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, ThemeToggle],
  template: `
    <div class="min-h-screen bg-[var(--surface-base)] text-[var(--text-primary)]">
      <header
        class="sticky top-0 z-20 flex items-center justify-between h-14 px-6 md:px-8
               bg-[var(--surface-base)] border-b border-[var(--border-subtle)]"
      >
        <a routerLink="/roster" class="font-display font-semibold text-[1.3rem] uppercase tracking-[0.22em] text-[var(--text-primary)]">
          Atlas
        </a>
        <atlas-theme-toggle />
      </header>
      <main class="max-w-[1120px] mx-auto px-6 md:px-8 py-10">
        <ng-content />
      </main>
    </div>
  `,
})
export class RosterShell {}
