import { ChangeDetectionStrategy, Component, input, model } from '@angular/core';

export interface SegmentOption {
  value: string;
  label: string;
}

/**
 * SegmentedControl du design system (§4.12) : choix mutuellement exclusif compact (2–3 segments).
 * Le segment actif porte fond + ombre + texte primaire (la couleur n'est jamais seule porteuse de sens).
 * Accessible : `role="radiogroup"` / `role="radio"` + navigation flèches gauche/droite.
 */
@Component({
  selector: 'atlas-segmented-control',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div
      role="radiogroup"
      [attr.aria-label]="ariaLabel()"
      class="inline-flex p-0.5 rounded-lg bg-[var(--surface-sunken)] border border-[var(--border-default)]"
    >
      @for (opt of options(); track opt.value; let i = $index) {
        <button
          type="button"
          role="radio"
          [attr.aria-checked]="opt.value === value()"
          [tabindex]="opt.value === value() ? 0 : -1"
          (click)="value.set(opt.value)"
          (keydown)="onKeydown($event, i)"
          [class]="segmentClasses(opt.value === value())"
        >
          {{ opt.label }}
        </button>
      }
    </div>
  `,
})
export class AtlasSegmentedControl {
  readonly options = input.required<SegmentOption[]>();
  readonly ariaLabel = input<string>('');
  readonly value = model<string>('');

  protected segmentClasses(active: boolean): string {
    const base =
      'h-8 px-3 rounded-md font-sans text-body-sm cursor-pointer transition-colors duration-100 ' +
      'focus-visible:outline-none focus-visible:shadow-[var(--focus-ring)]';
    return active
      ? `${base} bg-[var(--surface-raised)] text-[var(--text-primary)] shadow-[var(--shadow-sm)]`
      : `${base} text-[var(--text-tertiary)] hover:text-[var(--text-primary)]`;
  }

  protected onKeydown(event: KeyboardEvent, index: number): void {
    const opts = this.options();
    let next = index;
    if (event.key === 'ArrowRight') next = (index + 1) % opts.length;
    else if (event.key === 'ArrowLeft') next = (index - 1 + opts.length) % opts.length;
    else return;
    event.preventDefault();
    this.value.set(opts[next].value);
  }
}
