import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { AtlasIcon } from './atlas-icon';

const BASE_INPUT =
  'w-full h-10 px-3 rounded-lg bg-[var(--surface-sunken)] text-[var(--text-primary)] font-sans ' +
  'text-[0.9375rem] border placeholder:text-[var(--text-disabled)] focus:outline-none ' +
  'focus:shadow-[var(--focus-ring)] transition-colors duration-150';

/**
 * Champ de saisie texte du design system (§4.2) : label (caption), input, et message d'aide OU
 * d'erreur. État erreur = bordure `danger` + message `danger` avec icône `alert-circle` (la couleur
 * n'est jamais seule porteuse de sens). Champs texte en `font-sans` (les champs numériques, eux,
 * seraient en `font-mono` — pas notre cas ici).
 */
@Component({
  selector: 'atlas-input',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AtlasIcon],
  template: `
    <div class="flex flex-col gap-1.5">
      <label [attr.for]="id" class="font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">
        {{ label() }}
      </label>
      <input
        [id]="id"
        [type]="type()"
        [attr.placeholder]="placeholder()"
        [attr.autocomplete]="autocomplete()"
        [attr.aria-invalid]="!!error()"
        [attr.aria-describedby]="error() ? id + '-msg' : null"
        [value]="value()"
        (input)="value.set(asValue($event))"
        [class]="inputClasses()"
      />
      @if (error()) {
        <p [id]="id + '-msg'" class="flex items-center gap-1 font-sans text-body-sm text-[var(--danger)]">
          <atlas-icon name="alert-circle" [size]="16" />
          {{ error() }}
        </p>
      } @else if (helper()) {
        <p class="font-sans text-body-sm text-[var(--text-tertiary)]">{{ helper() }}</p>
      }
    </div>
  `,
})
export class AtlasInput {
  private static seq = 0;
  protected readonly id = `atlas-input-${AtlasInput.seq++}`;

  readonly label = input.required<string>();
  readonly type = input<'text' | 'email'>('text');
  readonly placeholder = input<string>('');
  readonly autocomplete = input<string | null>(null);
  readonly helper = input<string | null>(null);
  readonly error = input<string | null>(null);

  readonly value = model<string>('');

  protected readonly inputClasses = computed(() => {
    const border = this.error()
      ? 'border-[var(--danger)]'
      : 'border-[var(--border-default)] hover:border-[var(--border-strong)] focus:border-[var(--accent)]';
    return `${BASE_INPUT} ${border}`;
  });

  protected asValue(event: Event): string {
    return (event.target as HTMLInputElement).value;
  }
}
