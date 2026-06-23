import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { FocusLayout } from '../../ui/focus-layout';
import { AtlasInput } from '../../ui/atlas-input';
import { AtlasButton } from '../../ui/atlas-button';

@Component({
  selector: 'atlas-onboarding-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FocusLayout, AtlasInput, AtlasButton],
  template: `
    <atlas-focus-layout>
      <div class="text-center">
        <p class="font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">Dernière étape</p>
        <h1 class="mt-3 font-display text-h1 uppercase text-[var(--text-primary)]">Ton nom de coach</h1>
        <p class="mx-auto mt-4 max-w-[34ch] font-sans text-body text-[var(--text-secondary)]">
          C'est sous ce nom que tu dirigeras ton écurie. Tu pourras le changer plus tard.
        </p>
      </div>

      <form class="mt-8 flex flex-col gap-4" (submit)="submit($event)">
        <atlas-input
          label="Ton nom"
          type="text"
          placeholder="Ryan"
          autocomplete="nickname"
          [(value)]="displayName"
          [error]="fieldError()"
        />

        @if (formError()) {
          <p class="font-sans text-body-sm text-[var(--danger)]">{{ formError() }}</p>
        }

        <atlas-button type="submit" variant="primary" [loading]="submitting()">Continuer</atlas-button>
      </form>
    </atlas-focus-layout>
  `,
})
export class OnboardingPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly displayName = signal('');
  protected readonly submitting = signal(false);
  protected readonly fieldError = signal<string | null>(null);
  protected readonly formError = signal<string | null>(null);

  protected submit(event: Event): void {
    event.preventDefault();
    this.fieldError.set(null);
    this.formError.set(null);

    const name = this.displayName().trim();
    if (name.length < 2) {
      this.fieldError.set('Ton nom doit faire au moins 2 caractères.');
      return;
    }

    this.submitting.set(true);
    this.auth.completeSignup(name).subscribe({
      next: () => this.router.navigate(['/home']),
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        if (err.status === 401) {
          // Session de vérification expirée : on repart du début.
          this.router.navigate(['/login']);
        } else if (err.status === 400) {
          this.fieldError.set(err.error?.error ?? 'Ce nom est invalide.');
        } else {
          this.formError.set('Impossible de finaliser pour le moment. Réessaie dans un moment.');
        }
      },
    });
  }
}
