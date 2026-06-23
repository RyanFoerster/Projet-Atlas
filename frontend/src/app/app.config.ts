import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
  provideZonelessChangeDetection,
} from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withXsrfConfiguration } from '@angular/common/http';
import { catchError, of } from 'rxjs';

import { routes } from './app.routes';
import { AuthService } from './auth/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Zoneless explicite (pas de zone.js) — change detection pilotée par les signals.
    provideZonelessChangeDetection(),
    provideRouter(routes),
    // Same-origin (ADR-018) → support XSRF natif d'Angular : lit le cookie XSRF-TOKEN et pose
    // l'en-tête X-XSRF-TOKEN sur les requêtes mutantes. Les noms correspondent aux défauts de Spring.
    provideHttpClient(withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' })),
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
