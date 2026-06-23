import { ApplicationConfig, provideBrowserGlobalErrorListeners, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { apiInterceptor } from './core/api.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    // Zoneless explicite (pas de zone.js) — change detection pilotée par les signals.
    provideZonelessChangeDetection(),
    provideRouter(routes),
    // Interceptor API : cookie de session cross-origin + en-tête CSRF.
    provideHttpClient(withInterceptors([apiInterceptor])),
  ]
};
