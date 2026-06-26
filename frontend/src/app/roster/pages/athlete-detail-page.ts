import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { RosterService } from '../roster.service';
import { Athlete, AthleteCondition } from '../roster.models';
import { MOVEMENT_LABELS, MOVEMENT_ORDER } from '../roster.labels';
import { movementLabel, relativeDate } from '../../training/training.labels';
import { RosterShell } from '../roster-shell';
import { AtlasGeneticsPanel } from '../genetics-panel';
import { AtlasRarityBadge } from '../../ui/atlas-rarity-badge';
import { AtlasIcon } from '../../ui/atlas-icon';
import { AtlasConditionGauge } from '../../ui/atlas-condition-gauge';

/** Fiche détaillée d'un athlète (`/roster/athletes/:id`). 404 si l'athlète n'est pas dans l'écurie. */
@Component({
  selector: 'atlas-athlete-detail-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RosterShell, AtlasGeneticsPanel, AtlasRarityBadge, AtlasIcon, AtlasConditionGauge, RouterLink],
  template: `
    <atlas-roster-shell>
      <a routerLink="/roster" class="inline-flex items-center gap-1.5 mb-6 font-sans text-body-sm text-[var(--text-tertiary)] hover:text-[var(--text-primary)] transition-colors">
        <atlas-icon name="arrow-left" [size]="16" />Écurie
      </a>

      @if (loading()) {
        <p class="font-sans text-body text-[var(--text-tertiary)]">Chargement de la fiche…</p>
      } @else if (error()) {
        <p class="font-sans text-body-sm text-[var(--danger)] flex items-center gap-2">
          <atlas-icon name="alert-circle" [size]="16" />{{ error() }}
        </p>
      } @else if (athlete(); as a) {
        <div class="flex items-start justify-between gap-3 flex-wrap mb-1.5">
          <div class="flex items-center gap-3">
            <h1 class="font-display font-semibold text-[2.125rem] leading-none uppercase tracking-[0.03em] text-[var(--text-primary)]">{{ a.name }}</h1>
            @if (a.mirror) {
              <span class="inline-flex items-center h-[20px] px-2 rounded-[5px] bg-[var(--accent-surface)] text-[var(--accent)] text-caption uppercase tracking-[0.06em]">Miroir</span>
            }
          </div>
          <atlas-rarity-badge [rarity]="a.rarity" />
        </div>
        <p class="mb-7 font-mono text-data text-[var(--text-tertiary)]">
          {{ a.age }} ans · {{ a.bodyWeightKg }} kg · {{ a.bodyHeightCm }} cm · {{ a.gender === 'MALE' ? 'Homme' : 'Femme' }}
        </p>

        @if (lifts().length) {
          <div class="grid gap-3 mb-8 [grid-template-columns:repeat(auto-fit,minmax(130px,1fr))]">
            @for (lift of lifts(); track lift.key) {
              <div class="rounded-xl bg-[var(--surface-base)] border border-[var(--border-subtle)] p-4">
                <div class="mb-2 font-sans text-caption uppercase tracking-[0.08em] text-[var(--text-tertiary)]">{{ lift.label }}</div>
                <div class="font-mono text-data-lg text-[var(--text-primary)]">{{ lift.value }}<span class="text-[1.05rem] text-[var(--text-tertiary)]">kg</span></div>
              </div>
            }
          </div>
        }

        @if (a.mirror && a.trainingHistory.workoutCount > 0 && condition(); as c) {
          <div class="mb-8 max-w-[640px]">
            <atlas-condition-gauge [condition]="c" />
          </div>
        }

        @if (a.mirror) {
          <div class="rounded-xl bg-[var(--surface-base)] border border-[var(--border-subtle)] p-5 mb-8 max-w-[640px]">
            <div class="mb-3 font-sans text-caption uppercase tracking-[0.08em] text-[var(--text-tertiary)]">Historique d'entraînement</div>
            @if (a.trainingHistory.workoutCount > 0) {
              <div class="font-mono tabular-nums text-data-lg text-[var(--text-primary)]">
                {{ a.trainingHistory.workoutCount }}<span class="text-[1.05rem] text-[var(--text-tertiary)]"> séance{{ a.trainingHistory.workoutCount > 1 ? 's' : '' }}</span>
              </div>
              @if (a.trainingHistory.lastWorkoutAt) {
                <div class="mt-2 font-sans text-body-sm text-[var(--text-secondary)]">Dernière séance : {{ relativeDate(a.trainingHistory.lastWorkoutAt) }}</div>
              }
              @if (a.trainingHistory.lastPatternsCovered.length) {
                <div class="mt-3 flex flex-wrap gap-1.5">
                  @for (p of a.trainingHistory.lastPatternsCovered; track p) {
                    <span class="inline-flex items-center h-[22px] px-2 rounded-md font-sans text-[0.6875rem] uppercase tracking-[0.06em] bg-[var(--surface-raised-2)] text-[var(--text-tertiary)] border border-[var(--border-default)]">{{ movementLabel(p) }}</span>
                  }
                </div>
              }
            } @else {
              <p class="font-sans text-body-sm text-[var(--text-secondary)]">
                Aucune séance loggée pour l'instant. <a routerLink="/training/log" class="text-[var(--accent)] hover:underline">Logge ta première séance.</a>
              </p>
            }
          </div>
        }

        <div class="rounded-xl bg-[var(--surface-raised)] border border-[var(--border-default)] p-6 max-w-[640px]">
          <p class="mb-5 font-sans font-semibold text-[1.05rem] text-[var(--text-primary)]">Profil génétique</p>
          <atlas-genetics-panel [genetics]="a.genetics" />
        </div>
      }
    </atlas-roster-shell>
  `,
})
export class AthleteDetailPage implements OnInit {
  private readonly service = inject(RosterService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly athlete = signal<Athlete | null>(null);
  protected readonly condition = signal<AthleteCondition | null>(null);
  protected readonly loading = signal(true);
  protected readonly error = signal<string | null>(null);

  protected readonly relativeDate = relativeDate;
  protected readonly movementLabel = movementLabel;

  protected readonly lifts = computed(() => {
    const a = this.athlete();
    if (!a) return [];
    return MOVEMENT_ORDER
      .filter((k) => a.oneRepMaxesKg[k] != null)
      .map((key) => ({ key, label: MOVEMENT_LABELS[key], value: a.oneRepMaxesKg[key] }));
  });

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) {
      this.router.navigate(['/roster']);
      return;
    }
    this.service.getAthlete(id).subscribe({
      next: (a) => {
        this.athlete.set(a);
        this.loading.set(false);
        // 2e fetch : la forme du miroir (module athletics). Dégradation gracieuse — un échec n'enlève
        // pas le reste de la fiche, la section Forme est simplement omise (doctrine async-states).
        if (a.mirror && a.trainingHistory.workoutCount > 0) {
          this.service.getAthleteCondition(a.id).subscribe({
            next: (c) => this.condition.set(c),
            error: () => {},
          });
        }
      },
      error: (err: HttpErrorResponse) => {
        this.loading.set(false);
        if (err.status === 401) {
          this.router.navigate(['/login']);
          return;
        }
        this.error.set(err.status === 404 ? 'Cet athlète est introuvable dans ton écurie.' : 'Impossible de charger la fiche.');
      },
    });
  }
}
