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
import { AtlasSelect, SelectOption } from '../../ui/atlas-select';
import { SetForm } from './set-form';

const CELL =
  'h-[34px] px-2.5 rounded-md bg-[var(--surface-sunken)] text-[var(--text-primary)] ' +
  'font-mono tabular-nums text-[0.9rem] border border-[var(--border-default)] ' +
  'hover:border-[var(--border-strong)] focus:outline-none focus:border-[var(--accent)] ' +
  'focus:shadow-[var(--focus-ring)] transition-colors duration-150';

/**
 * Sous-ligne d'une série (design system §4.13). Grille reps · charge · poids · rpe · supprimer. Le
 * <strong>type de charge est par série</strong> (mini-select compact, ADR-035 §6) : la cellule poids s'adapte
 * (kg / +kg / désactivée « Poids de corps »). RPE vide = non renseigné. Clavier : Enter ajoute une série (qui
 * hérite du type de charge de la précédente) ; Tab parcourt les champs.
 */
@Component({
  selector: 'atlas-exercise-set-row',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AtlasIcon, AtlasSelect],
  template: `
    <div class="grid grid-cols-[56px_140px_1fr_56px_28px] gap-2.5 items-center">
      <input
        #repsInput
        type="number" inputmode="numeric" min="1" max="100" aria-label="Répétitions"
        [value]="set().reps()" (input)="set().reps.set(val($event))" (keydown.enter)="onEnter($event)"
        [class]="cell + ' text-center'"
      />
      <atlas-select
        size="compact" ariaLabel="Type de charge" [options]="loadTypeOptions"
        [value]="set().loadType()" (valueChange)="set().loadType.set($any($event))"
      />
      <div class="relative">
        <input
          type="number" inputmode="decimal" min="0" step="0.5"
          [attr.aria-label]="set().loadType() === 'WEIGHTED' ? 'Charge ajoutée en kilogrammes' : 'Poids en kilogrammes'"
          [disabled]="set().loadType() === 'BODYWEIGHT'"
          [value]="set().loadType() === 'BODYWEIGHT' ? '' : set().weightKg()"
          (input)="set().weightKg.set(val($event))" (keydown.enter)="onEnter($event)"
          [placeholder]="set().loadType() === 'BODYWEIGHT' ? 'Poids de corps' : ''"
          [class]="cell + ' w-full pr-8' + (set().loadType() === 'BODYWEIGHT' ? ' opacity-50 cursor-not-allowed' : '')"
        />
        @if (set().loadType() !== 'BODYWEIGHT') {
          <span class="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 font-mono text-[0.75rem] text-[var(--text-tertiary)]">{{ set().loadType() === 'WEIGHTED' ? '+kg' : 'kg' }}</span>
        }
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
  protected readonly loadTypeOptions: SelectOption[] = [
    { value: 'EXTERNAL', label: 'Externe' },
    { value: 'WEIGHTED', label: 'Lesté' },
    { value: 'BODYWEIGHT', label: 'Poids de corps' },
  ];

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
