import { HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { RosterService } from '../roster.service';
import { Gender } from '../roster.models';
import { FocusLayout } from '../../ui/focus-layout';
import { AtlasInput } from '../../ui/atlas-input';
import { AtlasButton } from '../../ui/atlas-button';
import { AtlasIcon } from '../../ui/atlas-icon';

/**
 * Création de l'athlète miroir (`/roster/mirror/new`) : acte fondateur de l'écurie. Le joueur saisit
 * son profil + ses vrais 1RM ; le backend en dérive une génétique hybride (boost des axes mesurés).
 * Gère 409 (miroir déjà créé → retour écurie), 400 (validation) et 401 (session).
 */
@Component({
  selector: 'atlas-mirror-create-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [FocusLayout, AtlasInput, AtlasButton, AtlasIcon],
  template: `
    <atlas-focus-layout>
      <div class="text-center">
        <p class="font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">Ton athlète miroir</p>
        <h1 class="mt-3 font-display text-h1 uppercase text-[var(--text-primary)]">Crée ton avatar</h1>
        <p class="mx-auto mt-4 max-w-[40ch] font-sans text-body text-[var(--text-secondary)]">
          Il progressera avec tes vraies séances. Tes 1RM mesurés orientent sa génétique de force.
        </p>
      </div>

      <form class="mt-8 flex flex-col gap-4" (submit)="submit($event)">
        <atlas-input label="Son nom" placeholder="Ryan" [(value)]="name" [error]="nameError()" />

        <div class="flex flex-col gap-1.5">
          <span class="font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">Sexe</span>
          <div class="grid grid-cols-2 gap-2">
            @for (g of genders; track g.value) {
              <button
                type="button"
                (click)="gender.set(g.value)"
                [class]="genderClass(g.value)"
              >{{ g.label }}</button>
            }
          </div>
        </div>

        <div class="grid grid-cols-2 gap-3">
          <atlas-input label="Âge" placeholder="30" [(value)]="age" />
          <atlas-input label="Poids (kg)" placeholder="80" [(value)]="bodyWeightKg" />
        </div>
        <atlas-input label="Taille (cm)" placeholder="178" [(value)]="bodyHeightCm" />

        <p class="mt-2 font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">Tes 1RM (kg)</p>
        <div class="grid grid-cols-2 gap-3">
          <atlas-input label="Squat" placeholder="140" [(value)]="squat" />
          <atlas-input label="Développé couché" placeholder="100" [(value)]="bench" />
          <atlas-input label="Soulevé de terre" placeholder="180" [(value)]="deadlift" />
          <atlas-input label="Développé militaire" placeholder="60" [(value)]="ohp" />
        </div>

        @if (formError()) {
          <p class="flex items-center gap-2 font-sans text-body-sm text-[var(--danger)]">
            <atlas-icon name="alert-circle" [size]="16" />{{ formError() }}
          </p>
        }

        <atlas-button type="submit" variant="primary" [loading]="submitting()">Créer mon athlète miroir</atlas-button>
      </form>
    </atlas-focus-layout>
  `,
})
export class MirrorCreatePage {
  private readonly service = inject(RosterService);
  private readonly router = inject(Router);

  protected readonly genders: { value: Gender; label: string }[] = [
    { value: 'MALE', label: 'Homme' },
    { value: 'FEMALE', label: 'Femme' },
  ];

  protected readonly name = signal('');
  protected readonly gender = signal<Gender>('MALE');
  protected readonly age = signal('');
  protected readonly bodyWeightKg = signal('');
  protected readonly bodyHeightCm = signal('');
  protected readonly squat = signal('');
  protected readonly bench = signal('');
  protected readonly deadlift = signal('');
  protected readonly ohp = signal('');

  protected readonly submitting = signal(false);
  protected readonly nameError = signal<string | null>(null);
  protected readonly formError = signal<string | null>(null);

  protected genderClass(value: Gender): string {
    const base =
      'h-10 rounded-lg font-sans font-semibold text-[0.9375rem] border transition-colors duration-150';
    return this.gender() === value
      ? `${base} bg-[var(--accent-surface)] border-[var(--accent)] text-[var(--text-primary)]`
      : `${base} bg-[var(--surface-raised)] border-[var(--border-default)] text-[var(--text-secondary)] hover:border-[var(--border-strong)]`;
  }

  protected submit(event: Event): void {
    event.preventDefault();
    this.nameError.set(null);
    this.formError.set(null);

    const name = this.name().trim();
    if (name.length < 2) {
      this.nameError.set('Le nom doit faire au moins 2 caractères.');
      return;
    }
    const nums = this.parseNumbers();
    if (!nums) {
      this.formError.set('Renseigne des nombres valides pour l\'âge, le poids, la taille et tes 1RM.');
      return;
    }

    this.submitting.set(true);
    this.service
      .createMirror({
        name,
        gender: this.gender(),
        age: nums.age,
        bodyWeightKg: nums.bodyWeightKg,
        bodyHeightCm: nums.bodyHeightCm,
        oneRepMaxes: {
          SQUAT: nums.squat,
          BENCH_PRESS: nums.bench,
          DEADLIFT: nums.deadlift,
          OVERHEAD_PRESS: nums.ohp,
        },
      })
      .subscribe({
        next: (athlete) => this.router.navigate(['/roster/athletes', athlete.id]),
        error: (err: HttpErrorResponse) => {
          this.submitting.set(false);
          if (err.status === 409) {
            this.formError.set('Tu as déjà créé ton athlète miroir.');
            setTimeout(() => this.router.navigate(['/roster']), 1200);
          } else if (err.status === 400) {
            this.formError.set(err.error?.error ?? 'Certaines valeurs sont invalides.');
          } else if (err.status === 401) {
            this.router.navigate(['/login']);
          } else {
            this.formError.set('Impossible de créer ton miroir pour le moment. Réessaie.');
          }
        },
      });
  }

  private parseNumbers(): {
    age: number; bodyWeightKg: number; bodyHeightCm: number;
    squat: number; bench: number; deadlift: number; ohp: number;
  } | null {
    const parse = (s: string) => {
      const n = Number(s.replace(',', '.').trim());
      return Number.isFinite(n) && n > 0 ? n : null;
    };
    const age = parse(this.age());
    const bodyWeightKg = parse(this.bodyWeightKg());
    const bodyHeightCm = parse(this.bodyHeightCm());
    const squat = parse(this.squat());
    const bench = parse(this.bench());
    const deadlift = parse(this.deadlift());
    const ohp = parse(this.ohp());
    if ([age, bodyWeightKg, bodyHeightCm, squat, bench, deadlift, ohp].some((v) => v === null)) {
      return null;
    }
    return { age: age!, bodyWeightKg: bodyWeightKg!, bodyHeightCm: bodyHeightCm!, squat: squat!, bench: bench!, deadlift: deadlift!, ohp: ohp! };
  }
}
