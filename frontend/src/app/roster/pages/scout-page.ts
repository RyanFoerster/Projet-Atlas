import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { RosterService } from '../roster.service';
import { ScoutResponse } from '../roster.models';
import { MOVEMENT_LABELS, MOVEMENT_ORDER } from '../roster.labels';
import { RosterShell } from '../roster-shell';
import { AtlasGeneticsPanel } from '../genetics-panel';
import { AtlasRarityBadge } from '../../ui/atlas-rarity-badge';
import { AtlasButton } from '../../ui/atlas-button';
import { AtlasIcon } from '../../ui/atlas-icon';

/**
 * Scouting (`/roster/scout`). À l'arrivée on propose un candidat ; le joueur voit sa rareté et sa
 * génétique COMPLÈTE pour décider. « Recruter » l'ajoute (404 = expiré → on en propose un autre),
 * « Refuser » en propose un nouveau.
 */
@Component({
  selector: 'atlas-scout-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RosterShell, AtlasGeneticsPanel, AtlasRarityBadge, AtlasButton, AtlasIcon, RouterLink],
  template: `
    <atlas-roster-shell>
      <a routerLink="/roster" class="inline-flex items-center gap-1.5 mb-6 font-sans text-body-sm text-[var(--text-tertiary)] hover:text-[var(--text-primary)] transition-colors">
        <atlas-icon name="arrow-left" [size]="16" />Écurie
      </a>

      <p class="font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">Scouting</p>
      <h1 class="mt-2 mb-6 font-display text-h1 uppercase text-[var(--text-primary)]">Recrutement</h1>

      @if (notice()) {
        <p class="mb-4 flex items-center gap-2 font-sans text-body-sm text-[var(--warning)]">
          <atlas-icon name="alert-circle" [size]="16" />{{ notice() }}
        </p>
      }

      @if (scouting()) {
        <p class="font-sans text-body text-[var(--text-tertiary)]">Recherche d'un athlète…</p>
      } @else if (result(); as r) {
        <div class="max-w-[600px] rounded-xl bg-[var(--surface-raised)] border border-[var(--border-default)] p-6 shadow-[var(--shadow-md)]">
          <div class="flex items-start justify-between gap-3 mb-1">
            <span class="font-display font-semibold text-[1.6rem] leading-tight text-[var(--text-primary)]">{{ r.candidate.name }}</span>
            <atlas-rarity-badge [rarity]="r.candidate.rarity" />
          </div>
          <p class="mb-5 font-mono text-data text-[var(--text-tertiary)]">
            {{ r.candidate.age }} ans · {{ r.candidate.bodyWeightKg }} kg · {{ r.candidate.bodyHeightCm }} cm
          </p>

          <div class="mb-5 flex flex-wrap gap-x-5 gap-y-1.5 font-mono text-data text-[var(--text-secondary)]">
            @for (lift of lifts; track lift.key) {
              <span>{{ lift.label }} <span class="text-[var(--text-primary)]">{{ r.candidate.oneRepMaxesKg[lift.key] }}</span><span class="text-[var(--text-tertiary)]">kg</span></span>
            }
          </div>

          <atlas-genetics-panel [genetics]="r.candidate.genetics" />

          <div class="flex gap-3 mt-6">
            <atlas-button variant="primary" [loading]="recruiting()" (click)="recruit(r)">Recruter</atlas-button>
            <atlas-button variant="secondary" [disabled]="recruiting()" (click)="refuse()">Refuser</atlas-button>
          </div>
        </div>
      } @else if (error()) {
        <p class="font-sans text-body-sm text-[var(--danger)] flex items-center gap-2">
          <atlas-icon name="alert-circle" [size]="16" />{{ error() }}
        </p>
        <atlas-button class="mt-4 inline-block" variant="secondary" (click)="scout()">Réessayer</atlas-button>
      }
    </atlas-roster-shell>
  `,
})
export class ScoutPage implements OnInit {
  private readonly service = inject(RosterService);
  private readonly router = inject(Router);

  protected readonly lifts = MOVEMENT_ORDER.filter((k) => k !== 'ROW' && k !== 'CHIN_UP')
    .map((key) => ({ key, label: MOVEMENT_LABELS[key] }));

  protected readonly result = signal<ScoutResponse | null>(null);
  protected readonly scouting = signal(true);
  protected readonly recruiting = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly notice = signal<string | null>(null);

  ngOnInit(): void {
    this.scout();
  }

  protected scout(): void {
    this.scouting.set(true);
    this.error.set(null);
    this.result.set(null);
    this.service.scout().subscribe({
      next: (r) => {
        this.result.set(r);
        this.scouting.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.scouting.set(false);
        if (err.status === 401) {
          this.router.navigate(['/login']);
          return;
        }
        this.error.set('Le scouting a échoué. Réessaie dans un moment.');
      },
    });
  }

  protected refuse(): void {
    this.notice.set(null);
    this.scout();
  }

  protected recruit(r: ScoutResponse): void {
    this.recruiting.set(true);
    this.notice.set(null);
    this.service.recruit(r.candidateId).subscribe({
      next: (athlete) => this.router.navigate(['/roster/athletes', athlete.id]),
      error: (err: HttpErrorResponse) => {
        this.recruiting.set(false);
        if (err.status === 404) {
          this.notice.set('Ce candidat n\'est plus disponible. En voici un autre.');
          this.scout();
        } else if (err.status === 401) {
          this.router.navigate(['/login']);
        } else {
          this.notice.set('Le recrutement a échoué. Réessaie.');
        }
      },
    });
  }
}
