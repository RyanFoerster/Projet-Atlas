import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { WorkoutHistoryItem } from '../training.models';
import { absoluteDateTime, movementLabel, relativeDate } from '../training.labels';

/**
 * Carte d'une séance dans l'historique (design system §4.15). Card interactive → détail. Date relative
 * (display) + absolue (mono), patterns de force en badges neutres (accessoires non listés, cohérent
 * `patternsCovered`), méta exercices/séries/reps.
 */
@Component({
  selector: 'atlas-workout-session-card',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <a
      [routerLink]="['/training/sessions', session().id]"
      class="block text-left rounded-xl bg-[var(--surface-raised)] border border-[var(--border-default)] p-[18px]
             hover:border-[var(--border-strong)] hover:shadow-[var(--shadow-md)]
             focus-visible:outline-none focus-visible:shadow-[var(--focus-ring)] transition duration-150"
    >
      <div class="flex items-baseline justify-between gap-3 mb-2.5">
        <div class="flex items-baseline gap-2.5">
          <span class="font-display font-semibold text-[1.2rem] text-[var(--text-primary)]">{{ relative() }}</span>
          <span class="font-mono text-[0.75rem] text-[var(--text-tertiary)]">{{ absolute() }}</span>
        </div>
        @if (session().durationMinutes !== null) {
          <span class="font-mono text-data text-[var(--text-secondary)]">{{ session().durationMinutes }} min</span>
        }
      </div>
      @if (session().patternsCovered.length) {
        <div class="flex flex-wrap gap-1.5 mb-2.5">
          @for (p of session().patternsCovered; track p) {
            <span class="inline-flex items-center h-[22px] px-2 rounded-md font-sans text-[0.6875rem] uppercase tracking-[0.06em]
                         bg-[var(--surface-raised-2)] text-[var(--text-tertiary)] border border-[var(--border-default)]">
              {{ label(p) }}
            </span>
          }
        </div>
      }
      <div class="font-mono text-[0.75rem] text-[var(--text-tertiary)]">
        {{ session().exerciseCount }} exercice{{ session().exerciseCount > 1 ? 's' : '' }}
        · {{ session().totalSets }} séries · {{ session().totalReps }} reps
      </div>
    </a>
  `,
})
export class AtlasWorkoutSessionCard {
  readonly session = input.required<WorkoutHistoryItem>();

  protected readonly relative = computed(() => relativeDate(this.session().performedAt));
  protected readonly absolute = computed(() => absoluteDateTime(this.session().performedAt));

  protected label(pattern: string): string {
    return movementLabel(pattern);
  }
}
