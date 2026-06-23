import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { FocusLayout } from '../../ui/focus-layout';
import { AtlasButton } from '../../ui/atlas-button';
import { AtlasIcon } from '../../ui/atlas-icon';

@Component({
  selector: 'atlas-home-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FocusLayout, AtlasButton, AtlasIcon],
  template: `
    <atlas-focus-layout>
      <div class="text-center">
        <p class="font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">Ton écurie</p>
        <h1 class="mt-3 font-display text-h1 uppercase text-[var(--text-primary)]">
          Bonjour {{ user()?.displayName }}
        </h1>
        <p class="mx-auto mt-4 max-w-[36ch] font-sans text-body text-[var(--text-secondary)]">
          Te voilà connecté. Le tableau de bord de ton écurie se construira au fil des prochains sprints —
          recrutement, programmes, compétitions.
        </p>

        <div class="mt-8 flex justify-center">
          <atlas-button variant="secondary" (click)="logout()">
            <atlas-icon name="log-out" [size]="20" />
            Se déconnecter
          </atlas-button>
        </div>
      </div>
    </atlas-focus-layout>
  `,
})
export class HomePage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly user = this.auth.currentUser;

  protected logout(): void {
    this.auth.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login']),
    });
  }
}
