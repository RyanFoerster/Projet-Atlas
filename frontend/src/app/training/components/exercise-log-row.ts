import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { AtlasIcon } from '../../ui/atlas-icon';
import { AtlasSegmentedControl, SegmentOption } from '../../ui/atlas-segmented-control';
import { AtlasSelect } from '../../ui/atlas-select';
import { BODY_REGION_OPTIONS, MOVEMENT_OPTIONS } from '../training.labels';
import { AtlasExerciseSetRow } from './exercise-set-row';
import { ExerciseCategory, ExerciseForm, SetForm, newSet } from './set-form';

const NAME_INPUT =
  'flex-1 min-w-[140px] h-9 px-3 rounded-lg bg-[var(--surface-sunken)] text-[var(--text-primary)] ' +
  'font-sans text-[0.9375rem] border border-[var(--border-default)] placeholder:text-[var(--text-disabled)] ' +
  'hover:border-[var(--border-strong)] focus:outline-none focus:border-[var(--accent)] ' +
  'focus:shadow-[var(--focus-ring)] transition-colors duration-150';

/**
 * Bloc d'un exercice dans le logger (design system §4.14). Le `SegmentedControl` Composé/Accessoire pilote
 * le `Select` (MovementPattern ↔ BodyRegion) et le **liseré gauche** (bronze = composé, neutre = accessoire),
 * qui rend la nature de l'exercice scannable dans une longue séance.
 */
@Component({
  selector: 'atlas-exercise-log-row',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AtlasSegmentedControl, AtlasSelect, AtlasExerciseSetRow, AtlasIcon],
  template: `
    <div
      class="rounded-xl bg-[var(--surface-raised)] border border-[var(--border-default)] p-4 transition-colors"
      [style.border-left]="'2px solid ' + ruleColor()"
    >
      <div class="flex flex-wrap items-center gap-2.5 mb-3.5">
        <atlas-segmented-control
          ariaLabel="Type d'exercice"
          [options]="categoryOptions"
          [value]="exercise().category()"
          (valueChange)="onCategory($event)"
        />
        <input
          [value]="exercise().name()" (input)="exercise().name.set(val($event))"
          placeholder="Nom de l'exercice" aria-label="Nom de l'exercice" [class]="nameInput"
        />
        <div class="w-[160px]">
          @if (exercise().category() === 'COMPOUND_FORCE') {
            <atlas-select
              ariaLabel="Mouvement" [options]="movementOptions"
              [value]="exercise().patternKey()" (valueChange)="exercise().patternKey.set($event)"
            />
          } @else {
            <atlas-select
              ariaLabel="Région" [options]="regionOptions"
              [value]="exercise().regionKey()" (valueChange)="exercise().regionKey.set($event)"
            />
          }
        </div>
      </div>

      <div class="grid grid-cols-[56px_140px_1fr_56px_28px] gap-2.5 px-0.5 mb-1.5 font-sans text-[0.625rem] uppercase tracking-[0.08em] text-[var(--text-tertiary)]">
        <span>Reps</span><span>Charge</span><span>Poids</span><span>RPE</span><span></span>
      </div>
      <div class="flex flex-col gap-2">
        @for (s of exercise().sets(); track s) {
          <atlas-exercise-set-row [set]="s" (addSet)="addSet()" (remove)="removeSet(s)" />
        }
      </div>

      <div class="flex items-center justify-between mt-3">
        <button type="button" (click)="addSet()" class="flex items-center gap-1 font-sans text-body-sm text-[var(--accent)] hover:underline">
          <atlas-icon name="plus" [size]="16" />ajouter une série
        </button>
        <button type="button" (click)="remove.emit()" class="flex items-center gap-1 font-sans text-body-sm text-[var(--text-tertiary)] hover:text-[var(--danger)] transition-colors">
          <atlas-icon name="trash-2" [size]="16" />supprimer l'exercice
        </button>
      </div>
    </div>
  `,
})
export class AtlasExerciseLogRow {
  protected readonly nameInput = NAME_INPUT;
  protected readonly categoryOptions: SegmentOption[] = [
    { value: 'COMPOUND_FORCE', label: 'Composé' },
    { value: 'ACCESSORY', label: 'Accessoire' },
  ];
  protected readonly movementOptions = MOVEMENT_OPTIONS;
  protected readonly regionOptions = BODY_REGION_OPTIONS;

  readonly exercise = input.required<ExerciseForm>();
  readonly remove = output<void>();

  protected readonly ruleColor = computed(() =>
    this.exercise().category() === 'COMPOUND_FORCE' ? 'var(--accent)' : 'var(--border-strong)',
  );

  protected onCategory(value: string): void {
    this.exercise().category.set(value as ExerciseCategory);
  }

  protected addSet(): void {
    const sets = this.exercise().sets();
    const last = sets[sets.length - 1];
    // Héritage : la nouvelle série reprend type de charge / poids de la précédente (RPE vidé, focus).
    this.exercise().sets.set([...sets, newSet(last, true)]);
  }

  protected removeSet(target: SetForm): void {
    const sets = this.exercise().sets();
    if (sets.length <= 1) return; // toujours au moins une série
    this.exercise().sets.set(sets.filter((s) => s !== target));
  }

  protected val(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }
}
