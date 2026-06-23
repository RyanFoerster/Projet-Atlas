import { ChangeDetectionStrategy, Component } from '@angular/core';
import { ThemeToggle } from '../theme/theme-toggle';

/**
 * Layout « Focus » du design system (§5.2) : colonne centrée étroite, fond surface-base, pas de
 * sidebar — pour login, onboarding, paramètres. Le toggle de thème est en haut à droite. Le
 * contenu est projeté.
 */
@Component({
  selector: 'atlas-focus-layout',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ThemeToggle],
  template: `
    <main class="flex min-h-screen flex-col">
      <header class="flex h-14 items-center justify-end px-6">
        <atlas-theme-toggle />
      </header>
      <section class="flex flex-1 items-center justify-center px-6 pb-24">
        <div class="w-full max-w-[420px]">
          <ng-content />
        </div>
      </section>
    </main>
  `,
})
export class FocusLayout {}
