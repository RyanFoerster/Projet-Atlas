import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

export type AtlasButtonVariant = 'primary' | 'secondary' | 'ghost';
export type AtlasButtonType = 'button' | 'submit';

const BASE =
  'inline-flex items-center justify-center gap-2 h-10 px-4 rounded-lg font-sans font-semibold ' +
  'text-[0.9375rem] disabled:opacity-40 disabled:pointer-events-none ' +
  'focus-visible:outline-none focus-visible:shadow-[var(--focus-ring)] transition-colors duration-150';

const VARIANTS: Record<AtlasButtonVariant, string> = {
  primary:
    'bg-[var(--accent)] text-[var(--accent-text-on)] hover:bg-[var(--accent-hover)] active:bg-[var(--accent-active)]',
  secondary:
    'bg-[var(--surface-raised)] text-[var(--text-primary)] border border-[var(--border-default)] ' +
    'hover:border-[var(--border-strong)] hover:bg-[var(--surface-raised-2)] active:bg-[var(--surface-sunken)]',
  ghost:
    'bg-transparent text-[var(--text-secondary)] hover:bg-[var(--accent-surface)] hover:text-[var(--text-primary)]',
};

/**
 * Bouton du design system (§4.1). Variantes primary/secondary/ghost, état `loading` (spinner +
 * `aria-busy`, largeur figée pour éviter le saut). Police sans, jamais Cormorant.
 */
@Component({
  selector: 'atlas-button',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <button
      [type]="type()"
      [disabled]="disabled() || loading()"
      [attr.aria-busy]="loading()"
      [class]="classes()"
    >
      @if (loading()) {
        <svg class="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden="true">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 0 1 8-8v4a4 4 0 0 0-4 4H4z" />
        </svg>
      }
      <ng-content />
    </button>
  `,
})
export class AtlasButton {
  readonly variant = input<AtlasButtonVariant>('primary');
  readonly type = input<AtlasButtonType>('button');
  readonly loading = input(false);
  readonly disabled = input(false);

  protected readonly classes = computed(() => `${BASE} ${VARIANTS[this.variant()]}`);
}
