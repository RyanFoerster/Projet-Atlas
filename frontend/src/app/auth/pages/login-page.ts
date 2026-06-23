import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';
import { FocusLayout } from '../../ui/focus-layout';
import { AtlasInput } from '../../ui/atlas-input';
import { AtlasButton } from '../../ui/atlas-button';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

@Component({
  selector: 'atlas-login-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FocusLayout, AtlasInput, AtlasButton],
  template: `
    <atlas-focus-layout>
      <div class="text-center">
        <p class="font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">Connexion</p>
        <h1 class="mt-3 font-display text-h1 uppercase text-[var(--text-primary)]">Atlas</h1>
        <p class="mx-auto mt-4 max-w-[34ch] font-sans text-body text-[var(--text-secondary)]">
          Reprends ton écurie là où tu l'as laissée. Saisis ton email, on t'envoie un lien de connexion.
        </p>
      </div>

      <form class="mt-8 flex flex-col gap-4" (submit)="submit($event)">
        <atlas-input
          label="Email"
          type="email"
          placeholder="toi@exemple.com"
          autocomplete="email"
          [(value)]="email"
          [error]="fieldError()"
        />

        @if (formError()) {
          <p class="font-sans text-body-sm text-[var(--danger)]">{{ formError() }}</p>
        }

        <atlas-button type="submit" variant="primary" [loading]="submitting()">
          Recevoir le lien
        </atlas-button>
      </form>
    </atlas-focus-layout>
  `,
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly email = signal('');
  protected readonly submitting = signal(false);
  protected readonly fieldError = signal<string | null>(null);
  protected readonly formError = signal<string | null>(null);

  protected submit(event: Event): void {
    event.preventDefault();
    this.fieldError.set(null);
    this.formError.set(null);

    const email = this.email().trim();
    if (!EMAIL_RE.test(email)) {
      this.fieldError.set('Saisis une adresse email valide.');
      return;
    }

    this.submitting.set(true);
    this.auth.requestMagicLink(email).subscribe({
      next: () => this.router.navigate(['/login/sent'], { queryParams: { email } }),
      error: () => {
        this.submitting.set(false);
        this.formError.set("Impossible d'envoyer le lien pour l'instant. Réessaie dans un moment.");
      },
    });
  }
}
