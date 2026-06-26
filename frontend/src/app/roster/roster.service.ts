import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Athlete, AthleteCondition, CreateMirrorRequest, Roster, ScoutResponse } from './roster.models';

// Same-origin (ADR-018). XSRF posé automatiquement par withXsrfConfiguration (app.config).
const ROSTER_API = '/api/roster';

/**
 * Appels au backend roster + cache léger de l'écurie courante (signal). Les pages s'abonnent aux
 * Observables pour gérer leurs propres états async (pattern par composition, async-states.md).
 */
@Injectable({ providedIn: 'root' })
export class RosterService {
  private readonly http = inject(HttpClient);

  private readonly _roster = signal<Roster | null>(null);
  readonly roster = this._roster.asReadonly();

  /** L'écurie du joueur. 404 si pas encore créée (→ la page redirige vers la création du miroir). */
  getRoster(): Observable<Roster> {
    return this.http.get<Roster>(ROSTER_API).pipe(tap((r) => this._roster.set(r)));
  }

  createMirror(request: CreateMirrorRequest): Observable<Athlete> {
    return this.http.post<Athlete>(`${ROSTER_API}/mirror`, request);
  }

  getAthlete(id: string): Observable<Athlete> {
    return this.http.get<Athlete>(`${ROSTER_API}/athletes/${id}`);
  }

  /**
   * État de forme Banister d'un athlète (servi par le module athletics, endpoint dédié — pas de
   * composition backend pour éviter un cycle Roster↔Athletics, ADR-027). Composition côté frontend.
   */
  getAthleteCondition(id: string): Observable<AthleteCondition> {
    return this.http.get<AthleteCondition>(`/api/athletes/${id}/condition`);
  }

  /** Propose un candidat (non recruté). 200, non adressable. */
  scout(): Observable<ScoutResponse> {
    return this.http.post<ScoutResponse>(`${ROSTER_API}/scout`, {});
  }

  /** Recrute par id (anti-forge). 404 si expiré/déjà recruté. */
  recruit(candidateId: string): Observable<Athlete> {
    return this.http.post<Athlete>(`${ROSTER_API}/recruit`, { candidateId });
  }
}
