import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { RosterShell } from '../../roster/roster-shell';
import { AtlasButton } from '../../ui/atlas-button';
import { AtlasIcon } from '../../ui/atlas-icon';
import { AtlasWorkoutSessionCard } from '../components/workout-session-card';
import { WorkoutHistory } from '../training.models';
import { TrainingService } from '../training.service';

/**
 * Historique d'entraînement (`/training`) : liste chronologique des séances loggées, le plus récent
 * d'abord. États async par composition (async-states.md). Bouton « Logger une séance » → `/training/log`.
 */
@Component({
  selector: 'atlas-training-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RosterShell, AtlasButton, AtlasIcon, AtlasWorkoutSessionCard],
  template: `
    <atlas-roster-shell>
      <div class="flex items-end justify-between gap-4 mb-8 flex-wrap">
        <div>
          <p class="font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">Tes séances</p>
          <h1 class="mt-2 font-display text-h1 uppercase text-[var(--text-primary)]">Entraînement</h1>
        </div>
        <atlas-button variant="primary" (click)="goLog()">
          <atlas-icon name="plus" [size]="20" />Logger une séance
        </atlas-button>
      </div>

      @if (loading()) {
        <p class="font-sans text-body text-[var(--text-tertiary)]">Chargement de ton historique…</p>
      } @else if (error()) {
        <p class="font-sans text-body-sm text-[var(--danger)] flex items-center gap-2">
          <atlas-icon name="alert-circle" [size]="16" />{{ error() }}
        </p>
      } @else if (history(); as h) {
        @if (h.sessions.length) {
          <div class="grid gap-3 [grid-template-columns:repeat(auto-fill,minmax(320px,1fr))]">
            @for (s of h.sessions; track s.id) {
              <atlas-workout-session-card [session]="s" />
            }
          </div>
        } @else {
          <div class="max-w-[480px]">
            <p class="font-sans text-body text-[var(--text-secondary)]">
              Tu n'as pas encore loggé de séance. Ta première séance fera vivre ton athlète miroir.
            </p>
          </div>
        }
      }
    </atlas-roster-shell>
  `,
})
export class TrainingPage implements OnInit {
  private readonly service = inject(TrainingService);
  private readonly router = inject(Router);

  protected readonly history = signal<WorkoutHistory | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  ngOnInit(): void {
    this.service.getHistory(0, 20).subscribe({
      next: (h) => {
        this.history.set(h);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status === 401) {
          this.router.navigate(['/login']);
          return;
        }
        this.error.set('Impossible de charger ton historique. Réessaie dans un moment.');
      },
    });
  }

  protected goLog(): void {
    this.router.navigate(['/training/log']);
  }
}
