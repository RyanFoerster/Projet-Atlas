import { ChangeDetectionStrategy, Component, computed, input, model } from '@angular/core';
import { AtlasIcon } from './atlas-icon';

export interface SelectOption {
  value: string;
  label: string;
}

const BASE_SELECT =
  'w-full rounded-lg appearance-none bg-[var(--surface-sunken)] ' +
  'text-[var(--text-primary)] font-sans border focus:outline-none ' +
  'focus:shadow-[var(--focus-ring)] transition-colors duration-150 cursor-pointer';

// Tailles : `default` (h-10, identique à l'Input) ; `compact` (h-[34px], pour les lignes denses comme la
// ligne de série du logger — design system §4.11/§4.13).
const SIZE_CLASSES: Record<'default' | 'compact', string> = {
  default: 'h-10 pl-3 pr-9 text-[0.9375rem]',
  compact: 'h-[34px] pl-2.5 pr-8 text-[0.85rem]',
};

/**
 * Select du design system (§4.11) : `<select>` natif stylé comme l'Input — clavier + a11y gratuits,
 * zéro lib. Le chrome natif est masqué (`appearance-none`) et un chevron Lucide superposé. La bordure et le
 * focus bronze sont identiques à l'Input pour une cohérence parfaite. Deux tailles : `default` (h-10) et
 * `compact` (h-34, lignes denses).
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
      <span
        class="pointer-events-none absolute top-1/2 -translate-y-1/2 text-[var(--text-tertiary)]"
        [class]="size() === 'compact' ? 'right-2.5' : 'right-3'"
      >
        <atlas-icon name="chevron-down" [size]="16" />
      </span>
    </div>
  `,
})
export class AtlasSelect {
  readonly options = input.required<SelectOption[]>();
  readonly ariaLabel = input<string>('');
  readonly error = input<boolean>(false);
  readonly size = input<'default' | 'compact'>('default');
  readonly value = model<string>('');

  protected readonly selectClasses = computed(() => {
    const border = this.error()
      ? 'border-[var(--danger)]'
      : 'border-[var(--border-default)] hover:border-[var(--border-strong)] focus:border-[var(--accent)]';
    return `${BASE_SELECT} ${SIZE_CLASSES[this.size()]} ${border}`;
  });

  protected asValue(event: Event): string {
    return (event.target as HTMLSelectElement).value;
  }
}
