import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { RosterService } from '../roster.service';
import { Roster } from '../roster.models';
import { RosterShell } from '../roster-shell';
import { AtlasAthleteCard } from '../../ui/atlas-athlete-card';
import { AtlasButton } from '../../ui/atlas-button';
import { AtlasIcon } from '../../ui/atlas-icon';

/**
 * Page écurie (`/roster`). Si le joueur n'a pas encore d'écurie, le backend renvoie 404 → on redirige
 * vers la création du miroir (`/roster/mirror/new`) : le miroir est l'acte fondateur de l'écurie.
 */
@Component({
  selector: 'atlas-roster-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RosterShell, AtlasAthleteCard, AtlasButton, AtlasIcon],
  template: `
    <atlas-roster-shell>
      @if (loading()) {
        <p class="font-sans text-body text-[var(--text-tertiary)]">Chargement de ton écurie…</p>
      } @else if (error()) {
        <p class="font-sans text-body-sm text-[var(--danger)] flex items-center gap-2">
          <atlas-icon name="alert-circle" [size]="16" />{{ error() }}
        </p>
      } @else if (roster(); as r) {
        <div class="flex items-end justify-between gap-4 mb-8 flex-wrap">
          <div>
            <p class="font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">Ton écurie</p>
            <h1 class="mt-2 font-display text-h1 uppercase text-[var(--text-primary)]">
              {{ r.athletes.length }} athlète{{ r.athletes.length > 1 ? 's' : '' }}
            </h1>
          </div>
          <atlas-button variant="primary" (click)="goScout()">
            <atlas-icon name="search" [size]="20" />Scouter un athlète
          </atlas-button>
        </div>

        @if (r.athletes.length) {
          <div class="grid gap-4 [grid-template-columns:repeat(auto-fill,minmax(260px,1fr))]">
            @for (a of r.athletes; track a.id) {
              <atlas-athlete-card
                [id]="a.id" [name]="a.name" [rarity]="a.rarity"
                [mirror]="a.mirror" [age]="a.age" [bodyWeightKg]="a.bodyWeightKg"
              />
            }
          </div>
        } @else {
          <p class="font-sans text-body text-[var(--text-secondary)]">
            Ton écurie est prête mais vide. Scoute ton premier athlète pour la peupler.
          </p>
        }
      }
    </atlas-roster-shell>
  `,
})
export class RosterPage implements OnInit {
  private readonly service = inject(RosterService);
  private readonly router = inject(Router);

  protected readonly roster = signal<Roster | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.service.getRoster().subscribe({
      next: (r) => {
        this.roster.set(r);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          // Pas encore d'écurie → le miroir est l'acte fondateur.
          this.router.navigate(['/roster/mirror/new']);
          return;
        }
        if (err.status === 401) {
          this.router.navigate(['/login']);
          return;
        }
        this.error.set('Impossible de charger ton écurie. Réessaie dans un moment.');
        this.loading.set(false);
      },
    });
  }

  protected goScout(): void {
    this.router.navigate(['/roster/scout']);
  }
}
