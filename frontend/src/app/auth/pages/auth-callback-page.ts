import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../auth.service';
import { FocusLayout } from '../../ui/focus-layout';

/**
 * Cible du lien magique (`/auth/callback?token=…`). Consomme le jeton puis route :
 * nouvel utilisateur → onboarding ; existant → on charge le profil (la session est déjà
 * authentifiée côté serveur) → accueil. En cas d'échec, état d'erreur sobre.
 */
@Component({
  selector: 'atlas-auth-callback-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FocusLayout, RouterLink],
  template: `
    <atlas-focus-layout>
      <div class="text-center">
        @if (error()) {
          <h1 class="font-display text-h1 uppercase text-[var(--text-primary)]">Lien inutilisable</h1>
          <p class="mx-auto mt-4 max-w-[34ch] font-sans text-body text-[var(--text-secondary)]">{{ error() }}</p>
          <a
            routerLink="/login"
            class="mt-8 inline-block font-sans text-body-sm text-[var(--accent)] underline-offset-4 hover:underline"
          >
            Demander un nouveau lien
          </a>
        } @else {
          <span class="mx-auto flex h-10 w-10 items-center justify-center text-[var(--text-tertiary)]">
            <svg class="h-6 w-6 animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z" />
            </svg>
          </span>
          <p class="mt-4 font-sans text-body text-[var(--text-secondary)]">Connexion en cours…</p>
        }
      </div>
    </atlas-focus-layout>
  `,
})
export class AuthCallbackPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.error.set('Ce lien est incomplet. Demande un nouveau lien de connexion.');
      return;
    }

    this.auth.consume(token).subscribe({
      next: (result) => {
        if (result.newUser) {
          this.router.navigate(['/onboarding']);
        } else {
          this.auth.loadCurrentUser().subscribe({
            next: () => this.router.navigate(['/roster']),
            error: () => this.error.set('Connexion impossible. Demande un nouveau lien.'),
          });
        }
      },
      error: () => this.error.set('Ce lien est invalide ou a expiré.'),
    });
  }
}
