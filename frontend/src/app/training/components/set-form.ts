import { WritableSignal, signal } from '@angular/core';

export type ExerciseCategory = 'COMPOUND_FORCE' | 'ACCESSORY';

/**
 * État de formulaire d'une série — chaque champ est un signal (réactif, OnPush-friendly, sans copie
 * d'array). Les valeurs sont des chaînes (ce que rendent les inputs natifs) ; on parse au submit.
 */
export interface SetForm {
  reps: WritableSignal<string>;
  weightKg: WritableSignal<string>;
  rpe: WritableSignal<string>;
  autofocus: boolean; // focus les reps au premier rendu (série ajoutée au clavier)
}

export interface ExerciseForm {
  category: WritableSignal<ExerciseCategory>;
  name: WritableSignal<string>;
  patternKey: WritableSignal<string>; // MovementPattern si composé
  regionKey: WritableSignal<string>; // BodyRegion si accessoire
  sets: WritableSignal<SetForm[]>;
}

export function newSet(template?: SetForm, autofocus = false): SetForm {
  return {
    reps: signal(template ? template.reps() : ''),
    weightKg: signal(template ? template.weightKg() : ''),
    rpe: signal(''), // le RPE ne se duplique pas (effort propre à la série)
    autofocus,
  };
}

export function newExercise(firstPattern: string): ExerciseForm {
  return {
    category: signal<ExerciseCategory>('COMPOUND_FORCE'),
    name: signal(''),
    patternKey: signal(firstPattern),
    regionKey: signal('CHEST'),
    sets: signal<SetForm[]>([newSet()]),
  };
}
