import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { CurrentUser, ConsumeResult } from './auth.models';

// URL relative same-origin. En dev, le proxy Angular (proxy.conf.json) renvoie /api vers :8080 ;
// en prod, frontend et backend sont sur la même origine (ADR-018). Du coup la paramétrisation via
// environment.ts qu'on avait prévue en TODO n'est PAS nécessaire pour le cas standard : elle ne
// redeviendrait utile que si le backend déménageait sur un autre domaine, ce qui n'est pas planifié.
const AUTH_API = '/api/auth';

/**
 * État d'authentification et appels au backend Identity. Signals-first : `currentUser` est la
 * source de vérité réactive consommée par les pages et le guard.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly _currentUser = signal<CurrentUser | null>(null);
  readonly currentUser = this._currentUser.asReadonly();
  readonly isAuthenticated = computed(() => this._currentUser() !== null);

  /** Demande un lien magique (toujours 202 côté backend, anti-énumération). */
  requestMagicLink(email: string): Observable<void> {
    return this.http.post<void>(`${AUTH_API}/magic-link/request`, { email });
  }

  /** Consomme le jeton du lien. N'établit pas encore l'état local (voir loadCurrentUser). */
  consume(token: string): Observable<ConsumeResult> {
    return this.http.get<ConsumeResult>(`${AUTH_API}/magic-link/consume`, { params: { token } });
  }

  /** Finalise l'inscription d'un nouveau Player et le mémorise localement. */
  completeSignup(displayName: string): Observable<CurrentUser> {
    return this.http
      .post<CurrentUser>(`${AUTH_API}/complete-signup`, { displayName })
      .pipe(tap((user) => this._currentUser.set(user)));
  }

  /** Charge le Player courant depuis la session (réhydratation au refresh). */
  loadCurrentUser(): Observable<CurrentUser> {
    return this.http.get<CurrentUser>(`${AUTH_API}/me`).pipe(tap((user) => this._currentUser.set(user)));
  }

  /** Détruit la session côté serveur et l'état local. */
  logout(): Observable<void> {
    return this.http.post<void>(`${AUTH_API}/logout`, {}).pipe(tap(() => this._currentUser.set(null)));
  }
}
