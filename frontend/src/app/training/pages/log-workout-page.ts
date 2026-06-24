import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { RosterShell } from '../../roster/roster-shell';
import { AtlasButton } from '../../ui/atlas-button';
import { AtlasIcon } from '../../ui/atlas-icon';
import { AtlasExerciseLogRow } from '../components/exercise-log-row';
import { ExerciseForm, newExercise } from '../components/set-form';
import { MOVEMENT_OPTIONS } from '../training.labels';
import { LogWorkoutRequest } from '../training.models';
import { TrainingService } from '../training.service';

const META_INPUT =
  'h-10 px-3 rounded-lg bg-[var(--surface-sunken)] text-[var(--text-primary)] border border-[var(--border-default)] ' +
  'hover:border-[var(--border-strong)] focus:outline-none focus:border-[var(--accent)] focus:shadow-[var(--focus-ring)] transition-colors duration-150';

/**
 * Logger une séance (`/training/log`) : tableau dynamique d'exercices style Football Manager. Saisie
 * clavier (Tab entre champs, Enter ajoute une série en dupliquant la dernière), choix Composé/Accessoire
 * par exercice. Succès → redirection vers l'historique. Le `pattern` XOR `region` est garanti par le segment.
 */
@Component({
  selector: 'atlas-log-workout-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RosterShell, AtlasButton, AtlasIcon, AtlasExerciseLogRow],
  template: `
    <atlas-roster-shell>
      <a (click)="cancel()" class="inline-flex items-center gap-1.5 mb-6 font-sans text-body-sm text-[var(--text-tertiary)] hover:text-[var(--text-primary)] transition-colors cursor-pointer">
        <atlas-icon name="arrow-left" [size]="16" />Entraînement
      </a>
      <p class="font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">Nouvelle séance</p>
      <h1 class="mt-2 mb-8 font-display text-h1 uppercase text-[var(--text-primary)]">Logger une séance</h1>

      <div class="max-w-[680px]">
        <!-- Méta séance -->
        <div class="flex flex-wrap gap-5 mb-8">
          <div class="flex flex-col gap-1.5">
            <label class="font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">Date et heure</label>
            <input type="datetime-local" [value]="performedAt()" (input)="performedAt.set(val($event))" [class]="metaInput + ' font-mono text-[0.9rem]'" />
          </div>
          <div class="flex flex-col gap-1.5">
            <label class="font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">Durée (min)</label>
            <input type="number" inputmode="numeric" min="1" placeholder="—" [value]="duration()" (input)="duration.set(val($event))" [class]="metaInput + ' w-28 font-mono tabular-nums'" />
          </div>
          <div class="flex flex-col gap-1.5 flex-1 min-w-[200px]">
            <label class="font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">Notes (optionnel)</label>
            <input type="text" maxlength="500" placeholder="Sensations, contexte…" [value]="notes()" (input)="notes.set(val($event))" [class]="metaInput + ' w-full font-sans text-[0.9375rem]'" />
          </div>
        </div>

        <!-- Exercices -->
        <div class="flex flex-col gap-3.5 mb-5">
          @for (ex of exercises(); track ex) {
            <atlas-exercise-log-row [exercise]="ex" (remove)="removeExercise(ex)" />
          }
        </div>

        <button type="button" (click)="addExercise()"
          class="w-full h-11 mb-8 rounded-xl border border-dashed border-[var(--border-default)] text-[var(--text-secondary)]
                 hover:border-[var(--border-strong)] hover:text-[var(--text-primary)] transition-colors flex items-center justify-center gap-2 font-sans text-body">
          <atlas-icon name="plus" [size]="20" />Ajouter un exercice
        </button>

        @if (formError()) {
          <p class="mb-4 flex items-center gap-2 font-sans text-body-sm text-[var(--danger)]">
            <atlas-icon name="alert-circle" [size]="16" />{{ formError() }}
          </p>
        }

        <div class="flex gap-3">
          <atlas-button variant="primary" [loading]="submitting()" (click)="submit()">Enregistrer la séance</atlas-button>
          <atlas-button variant="secondary" [disabled]="submitting()" (click)="cancel()">Annuler</atlas-button>
        </div>
      </div>
    </atlas-roster-shell>
  `,
})
export class LogWorkoutPage {
  private readonly service = inject(TrainingService);
  private readonly router = inject(Router);

  protected readonly metaInput = META_INPUT;

  protected readonly performedAt = signal(this.toLocalInput(new Date()));
  protected readonly duration = signal('');
  protected readonly notes = signal('');
  protected readonly exercises = signal<ExerciseForm[]>([newExercise(MOVEMENT_OPTIONS[0].value)]);

  protected readonly submitting = signal(false);
  protected readonly formError = signal<string | null>(null);

  protected addExercise(): void {
    this.exercises.set([...this.exercises(), newExercise(MOVEMENT_OPTIONS[0].value)]);
  }

  protected removeExercise(target: ExerciseForm): void {
    this.exercises.set(this.exercises().filter((e) => e !== target));
  }

  protected submit(): void {
    const problem = this.validate();
    if (problem) {
      this.formError.set(problem);
      return;
    }
    this.submitting.set(true);
    this.formError.set(null);
    this.service.logWorkout(this.buildRequest()).subscribe({
      next: () => this.router.navigate(['/training']),
      error: (err: HttpErrorResponse) => {
        this.submitting.set(false);
        if (err.status === 401) {
          this.router.navigate(['/login']);
          return;
        }
        if (err.status === 400) {
          this.formError.set(err.error?.error ?? 'Séance invalide. Vérifie tes exercices.');
          return;
        }
        this.formError.set("L'enregistrement a échoué. Réessaie dans un moment.");
      },
    });
  }

  protected cancel(): void {
    this.router.navigate(['/training']);
  }

  /** Validation cliente légère ; le backend reste autoritaire (400). */
  private validate(): string | null {
    const exercises = this.exercises();
    if (exercises.length === 0) return 'Ajoute au moins un exercice à ta séance.';
    for (const ex of exercises) {
      if (ex.name().trim().length < 2) return "Chaque exercice doit avoir un nom (2 caractères minimum).";
      const sets = ex.sets();
      if (!sets.length || sets.some((s) => !(parseInt(s.reps(), 10) >= 1))) {
        return 'Chaque série doit indiquer au moins une répétition.';
      }
    }
    return null;
  }

  private buildRequest(): LogWorkoutRequest {
    return {
      performedAt: new Date(this.performedAt()).toISOString(),
      durationMinutes: this.intOrNull(this.duration()),
      notes: this.notes().trim() || null,
      exercises: this.exercises().map((ex) => ({
        name: ex.name().trim(),
        pattern: ex.category() === 'COMPOUND_FORCE' ? ex.patternKey() : null,
        region: ex.category() === 'ACCESSORY' ? ex.regionKey() : null,
        sets: ex.sets().map((s) => ({
          reps: parseInt(s.reps(), 10) || 0,
          weightKg: this.floatOrNull(s.weightKg()),
          rpe: this.floatOrNull(s.rpe()),
        })),
      })),
    };
  }

  protected val(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  private intOrNull(raw: string): number | null {
    const n = parseInt(raw, 10);
    return Number.isFinite(n) ? n : null;
  }

  private floatOrNull(raw: string): number | null {
    if (raw.trim() === '') return null;
    const n = parseFloat(raw);
    return Number.isFinite(n) ? n : null;
  }

  private toLocalInput(d: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }
}
