import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { catchError, of } from 'rxjs';

import { routes } from './app.routes';
import { apiInterceptor } from './core/api.interceptor';
import { AuthService } from './auth/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Zoneless explicite (pas de zone.js) — change detection pilotée par les signals.
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([apiInterceptor])),
    // Au démarrage : GET /me. Double rôle (ADR-018) :
    //  1. réhydrate la session si elle existe (currentUser peuplé) ;
    //  2. amorce le cookie XSRF-TOKEN, nécessaire au tout premier POST (login).
    // Un 401 (utilisateur non connecté) est un état NORMAL et attendu : on l'avale silencieusement,
    // currentUser reste null, le routing ira vers /login. Surtout pas d'erreur fatale qui bloquerait le boot.
    provideAppInitializer(() => {
      const auth = inject(AuthService);
      return auth.loadCurrentUser().pipe(catchError(() => of(null)));
    }),
  ],
};
