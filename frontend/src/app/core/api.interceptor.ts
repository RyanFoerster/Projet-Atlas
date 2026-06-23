import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Intercepteur HTTP de l'API Atlas.
 *
 * - {@code withCredentials} : envoie le cookie de session (JSESSIONID) — indispensable car en dev
 *   le frontend (4200) et le backend (8080) sont cross-origin, et Angular n'envoie pas les cookies
 *   cross-site par défaut.
 * - CSRF : sur les méthodes mutantes, joint le jeton du cookie {@code XSRF-TOKEN} dans l'en-tête
 *   {@code X-XSRF-TOKEN} (attendu par Spring Security, cf. SecurityConfig). On le fait à la main
 *   car le support XSRF natif d'Angular n'agit pas en cross-origin.
 */
export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  let request = req.clone({ withCredentials: true });

  const isMutating = !['GET', 'HEAD', 'OPTIONS'].includes(req.method.toUpperCase());
  if (isMutating) {
    const token = readCookie('XSRF-TOKEN');
    if (token) {
      request = request.clone({ setHeaders: { 'X-XSRF-TOKEN': token } });
    }
  }

  return next(request);
};

function readCookie(name: string): string | null {
  const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
  return match ? decodeURIComponent(match[1]) : null;
}
