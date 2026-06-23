import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Protège les routes nécessitant un Player connecté.
 *
 * Si l'état local est déjà authentifié → accès direct. Sinon (ex. refresh de page, où le signal
 * est vide mais un cookie de session peut exister), on tente une réhydratation via /me : succès →
 * accès ; échec (401) → redirection vers /login.
 */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  return auth.loadCurrentUser().pipe(
    map(() => true),
    catchError(() => of(router.createUrlTree(['/login']))),
  );
};
