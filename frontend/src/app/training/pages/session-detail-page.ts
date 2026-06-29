import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { RosterShell } from '../../roster/roster-shell';
import { AtlasIcon } from '../../ui/atlas-icon';
import { ExerciseDetail, WorkoutSession } from '../training.models';
import { TrainingService } from '../training.service';
import { absoluteDateTime, bodyRegionLabel, movementLabel, relativeDate } from '../training.labels';

/**
 * Détail d'une séance loggée (`/training/sessions/:id`) : méta + tableau read-only des exercices et séries
 * (design system §4.6). 404 (inexistante ou pas à toi) → message + retour.
 */
@Component({
  selector: 'atlas-session-detail-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RosterShell, AtlasIcon, RouterLink],
  template: `
    <atlas-roster-shell>
      <a routerLink="/training" class="inline-flex items-center gap-1.5 mb-6 font-sans text-body-sm text-[var(--text-tertiary)] hover:text-[var(--text-primary)] transition-colors">
        <atlas-icon name="arrow-left" [size]="16" />Entraînement
      </a>

      @if (loading()) {
        <p class="font-sans text-body text-[var(--text-tertiary)]">Chargement de la séance…</p>
      } @else if (notFound()) {
        <p class="font-sans text-body text-[var(--text-secondary)]">Cette séance n'existe pas (ou n'est pas la tienne).</p>
      } @else if (error()) {
        <p class="font-sans text-body-sm text-[var(--danger)] flex items-center gap-2">
          <atlas-icon name="alert-circle" [size]="16" />{{ error() }}
        </p>
      } @else if (session(); as s) {
        <div class="flex items-baseline gap-3 flex-wrap">
          <h1 class="font-display text-h1 text-[var(--text-primary)]">{{ relativeDate(s.performedAt) }}</h1>
          <span class="font-mono text-data text-[var(--text-tertiary)]">{{ absoluteDateTime(s.performedAt) }}</span>
          @if (s.durationMinutes !== null) { <span class="font-mono text-data text-[var(--text-secondary)]">· {{ s.durationMinutes }} min</span> }
        </div>
        <p class="mt-2 mb-6 font-mono text-data text-[var(--text-tertiary)]">
          {{ s.exercises.length }} exercices · {{ s.totalSets }} séries · {{ s.totalReps }} reps · volume {{ s.estimatedVolumeKg }} kg
        </p>
        @if (s.notes) {
          <p class="mb-8 max-w-[600px] font-sans text-body text-[var(--text-secondary)] italic">« {{ s.notes }} »</p>
        }

        <div class="flex flex-col gap-7 max-w-[640px]">
          @for (ex of s.exercises; track $index) {
            <div>
              <div class="flex items-center gap-2.5 mb-2.5">
                <span class="font-display font-semibold text-[1.15rem] text-[var(--text-primary)]">{{ ex.name }}</span>
                <span [class]="tagClass(ex)">{{ categoryLabel(ex) }}</span>
              </div>
              <table class="w-full text-left border-collapse">
                <thead>
                  <tr class="border-b border-[var(--border-default)]">
                    <th class="py-2 px-2 font-sans text-caption uppercase tracking-[0.08em] text-[var(--text-tertiary)] font-medium">Série</th>
                    <th class="py-2 px-2 font-sans text-caption uppercase tracking-[0.08em] text-[var(--text-tertiary)] font-medium text-right">Reps</th>
                    <th class="py-2 px-2 font-sans text-caption uppercase tracking-[0.08em] text-[var(--text-tertiary)] font-medium text-right">Poids</th>
                    <th class="py-2 px-2 font-sans text-caption uppercase tracking-[0.08em] text-[var(--text-tertiary)] font-medium text-right">RPE</th>
                  </tr>
                </thead>
                <tbody>
                  @for (set of ex.sets; track $index; let i = $index) {
                    <tr class="border-b border-[var(--border-subtle)]">
                      <td class="py-2 px-2 font-mono text-body-sm text-[var(--text-tertiary)]">{{ i + 1 }}</td>
                      <td class="py-2 px-2 font-mono tabular-nums text-data text-[var(--text-secondary)] text-right">{{ set.reps }}</td>
                      <td class="py-2 px-2 font-mono tabular-nums text-data text-[var(--text-primary)] text-right">{{ set.loadType === 'BODYWEIGHT' ? 'PdC' : (set.loadType === 'WEIGHTED' ? '+' : '') + set.weightKg + ' kg' }}</td>
                      <td class="py-2 px-2 font-mono tabular-nums text-data text-[var(--text-secondary)] text-right">{{ set.rpe ?? '—' }}</td>
                    </tr>
                  }
                </tbody>
              </table>
            </div>
          }
        </div>
      }
    </atlas-roster-shell>
  `,
})
export class SessionDetailPage implements OnInit {
  private readonly service = inject(TrainingService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly session = signal<WorkoutSession | null>(null);
  protected readonly loading = signal(true);
  protected readonly notFound = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly relativeDate = relativeDate;
  protected readonly absoluteDateTime = absoluteDateTime;

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    this.service.getSession(id).subscribe({
      next: (s) => {
        this.session.set(s);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status === 404) {
          this.notFound.set(true);
          return;
        }
        if (err.status === 401) {
          this.router.navigate(['/login']);
          return;
        }
        this.error.set('Impossible de charger la séance. Réessaie dans un moment.');
      },
    });
  }

  protected categoryLabel(ex: ExerciseDetail): string {
    return ex.category === 'COMPOUND_FORCE' ? movementLabel(ex.pattern) : bodyRegionLabel(ex.region);
  }

  protected tagClass(ex: ExerciseDetail): string {
    const base = 'inline-flex items-center h-[20px] px-2 rounded-md font-sans text-[0.625rem] uppercase tracking-[0.06em]';
    return ex.category === 'COMPOUND_FORCE'
      ? `${base} bg-[var(--accent-surface)] text-[var(--accent)]`
      : `${base} bg-[var(--surface-raised-2)] text-[var(--text-tertiary)] border border-[var(--border-default)]`;
  }
}
