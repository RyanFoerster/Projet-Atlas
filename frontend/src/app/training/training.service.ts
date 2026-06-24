import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { LogWorkoutRequest, WorkoutHistory, WorkoutSession } from './training.models';

// Same-origin (ADR-018). XSRF posé automatiquement par withXsrfConfiguration (app.config).
const TRAINING_API = '/api/personal-training/sessions';

/**
 * Appels au backend PersonalTraining. Les pages s'abonnent aux Observables et gèrent leurs propres
 * états async (pattern par composition, async-states.md).
 */
@Injectable({ providedIn: 'root' })
export class TrainingService {
  private readonly http = inject(HttpClient);

  /** Logge une séance. 201 + détail ; 400 si invariants violés (séance vide, date future…). */
  logWorkout(request: LogWorkoutRequest): Observable<WorkoutSession> {
    return this.http.post<WorkoutSession>(TRAINING_API, request);
  }

  /** Historique paginé, le plus récent d'abord. */
  getHistory(page = 0, size = 20): Observable<WorkoutHistory> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<WorkoutHistory>(TRAINING_API, { params });
  }

  /** Détail d'une séance. 404 si inexistante ou pas à toi. */
  getSession(id: string): Observable<WorkoutSession> {
    return this.http.get<WorkoutSession>(`${TRAINING_API}/${id}`);
  }
}
