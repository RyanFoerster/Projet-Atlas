import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FocusLayout } from '../../ui/focus-layout';
import { AtlasIcon } from '../../ui/atlas-icon';

@Component({
  selector: 'atlas-link-sent-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FocusLayout, AtlasIcon, RouterLink],
  template: `
    <atlas-focus-layout>
      <div class="text-center">
        <span
          class="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-[var(--accent-surface)] text-[var(--accent)]"
        >
          <atlas-icon name="mail" [size]="24" />
        </span>

        <h1 class="mt-6 font-display text-h1 uppercase text-[var(--text-primary)]">Vérifie ta boîte mail</h1>

        <p class="mx-auto mt-4 max-w-[34ch] font-sans text-body text-[var(--text-secondary)]">
          On a envoyé un lien de connexion à
          <span class="font-mono text-[var(--text-primary)]">{{ email() }}</span>. Clique dessus pour entrer
          — il expire dans 15 minutes.
        </p>

        <a
          routerLink="/login"
          class="mt-8 inline-block font-sans text-body-sm text-[var(--text-tertiary)] underline-offset-4 hover:text-[var(--text-primary)] hover:underline"
        >
          Renvoyer un lien
        </a>
      </div>
    </atlas-focus-layout>
  `,
})
export class LinkSentPage {
  private readonly route = inject(ActivatedRoute);

  protected email(): string {
    return this.route.snapshot.queryParamMap.get('email') ?? 'ton adresse';
  }
}
