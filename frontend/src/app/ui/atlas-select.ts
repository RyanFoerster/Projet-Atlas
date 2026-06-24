import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { AtlasIcon } from './atlas-icon';

export interface SelectOption {
  value: string;
  label: string;
}

const BASE_SELECT =
  'w-full h-10 pl-3 pr-9 rounded-lg appearance-none bg-[var(--surface-sunken)] ' +
  'text-[var(--text-primary)] font-sans text-[0.9375rem] border focus:outline-none ' +
  'focus:shadow-[var(--focus-ring)] transition-colors duration-150 cursor-pointer';

/**
 * Select du design system (§4.11) : `<select>` natif stylé comme l'Input — clavier + a11y gratuits,
 * zéro lib. Le chrome natif est masqué (`appearance-none`) et un chevron Lucide superposé. La hauteur,
 * la bordure et le focus bronze sont identiques à l'Input pour une cohérence parfaite.
 */
@Component({
  selector: 'atlas-select',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AtlasIcon],
  template: `
    <div class="relative">
      <select
        [attr.aria-label]="ariaLabel()"
        [value]="value()"
        (change)="value.set(asValue($event))"
        [class]="selectClasses()"
      >
        @for (opt of options(); track opt.value) {
          <option [value]="opt.value">{{ opt.label }}</option>
        }
      </select>
      <span class="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-tertiary)]">
        <atlas-icon name="chevron-down" [size]="16" />
      </span>
    </div>
  `,
})
export class AtlasSelect {
  readonly options = input.required<SelectOption[]>();
  readonly ariaLabel = input<string>('');
  readonly error = input<boolean>(false);
  readonly value = model<string>('');

  protected readonly selectClasses = computed(() => {
    const border = this.error()
      ? 'border-[var(--danger)]'
      : 'border-[var(--border-default)] hover:border-[var(--border-strong)] focus:border-[var(--accent)]';
    return `${BASE_SELECT} ${border}`;
  });

  protected asValue(event: Event): string {
    return (event.target as HTMLSelectElement).value;
  }
}
