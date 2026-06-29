import { WritableSignal, signal } from '@angular/core';

export type ExerciseCategory = 'COMPOUND_FORCE' | 'ACCESSORY';

/**
 * Type de charge d'un exercice (sprint 6, ADR-035). Choisi au niveau de l'exercice (toutes ses séries
 * partagent le type — un « Weighted Pull-up » est lesté partout) ; le backend reste par-série.
 *  - `BODYWEIGHT` : poids de corps pur (pas de valeur de charge).
 *  - `WEIGHTED`   : lesté, la valeur saisie = charge AJOUTÉE.
 *  - `EXTERNAL`   : charge externe, la valeur saisie = charge externe.
 */
export type LoadType = 'BODYWEIGHT' | 'WEIGHTED' | 'EXTERNAL';

/**
 * État de formulaire d'une série — chaque champ est un signal (réactif, OnPush-friendly, sans copie
 * d'array). Les valeurs sont des chaînes (ce que rendent les inputs natifs) ; on parse au submit. Le
 * {@link LoadType} est <strong>par série</strong> (ADR-035 §6) : on peut logger des tractions lestées puis
 * une dernière série au poids de corps dans le même exercice.
 */
export interface SetForm {
  reps: WritableSignal<string>;
  loadType: WritableSignal<LoadType>; // poids de corps / lesté / externe (propre à la série)
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
    // Héritage : une nouvelle série reprend le type de charge (et le poids) de la précédente. On choisit
    // « Lesté » une fois, ça se propage ; on passe juste la dernière série en « Poids de corps » d'un tap.
    loadType: signal<LoadType>(template ? template.loadType() : 'EXTERNAL'),
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
