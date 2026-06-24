import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  afterNextRender,
  input,
  output,
  viewChild,
} from '@angular/core';
import { AtlasIcon } from '../../ui/atlas-icon';
import { SetForm } from './set-form';

const CELL =
  'h-[34px] px-2.5 rounded-md bg-[var(--surface-sunken)] text-[var(--text-primary)] ' +
  'font-mono tabular-nums text-[0.9rem] border border-[var(--border-default)] ' +
  'hover:border-[var(--border-strong)] focus:outline-none focus:border-[var(--accent)] ' +
  'focus:shadow-[var(--focus-ring)] transition-colors duration-150';

/**
 * Sous-ligne d'une série (design system §4.13). Grille reps · poids · rpe · supprimer, inputs mono.
 * Poids vide = poids de corps ; RPE vide = non renseigné. Clavier : Enter ajoute une série (l'`ExerciseLogRow`
 * duplique la dernière) ; Tab parcourt naturellement les champs.
 */
@Component({
  selector: 'atlas-exercise-set-row',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AtlasIcon],
  template: `
    <div class="grid grid-cols-[64px_1fr_64px_28px] gap-2.5 items-center">
      <input
        #repsInput
        type="number" inputmode="numeric" min="1" max="100" aria-label="Répétitions"
        [value]="set().reps()" (input)="set().reps.set(val($event))" (keydown.enter)="onEnter($event)"
        [class]="cell + ' text-center'"
      />
      <div class="relative">
        <input
          type="number" inputmode="decimal" min="0" step="0.5" aria-label="Poids en kilogrammes"
          [value]="set().weightKg()" (input)="set().weightKg.set(val($event))" (keydown.enter)="onEnter($event)"
          [class]="cell + ' w-full pr-8'"
        />
        <span class="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 font-mono text-[0.75rem] text-[var(--text-tertiary)]">kg</span>
      </div>
      <input
        type="number" inputmode="decimal" min="1" max="10" step="0.5" aria-label="RPE"
        [value]="set().rpe()" (input)="set().rpe.set(val($event))" (keydown.enter)="onEnter($event)"
        [class]="cell + ' text-center'"
      />
      <button
        type="button" aria-label="Supprimer la série" (click)="remove.emit()"
        class="flex items-center justify-center h-[34px] text-[var(--text-tertiary)] hover:text-[var(--danger)] transition-colors"
      >
        <atlas-icon name="x" [size]="16" />
      </button>
    </div>
  `,
})
export class AtlasExerciseSetRow {
  protected readonly cell = CELL;

  readonly set = input.required<SetForm>();
  readonly remove = output<void>();
  readonly addSet = output<void>();

  private readonly repsInput = viewChild<ElementRef<HTMLInputElement>>('repsInput');

  constructor() {
    afterNextRender(() => {
      if (this.set().autofocus) this.repsInput()?.nativeElement.focus();
    });
  }

  protected val(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }

  protected onEnter(event: Event): void {
    event.preventDefault(); // évite la soumission du formulaire
    this.addSet.emit();
  }
}
