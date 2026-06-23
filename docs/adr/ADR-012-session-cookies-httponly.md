# ADR-012 : Gestion de session par cookie HttpOnly (vs JWT)

**Statut** : Accepté
**Date** : Sprint 1
**Décideur** : Ryan Foerster

## Contexte

Une fois le Player authentifié (ADR-011), il faut maintenir son état de connexion entre les requêtes. Deux familles d'approches :

- **JWT (token stateless)** : un jeton signé renvoyé au client, présenté à chaque requête (souvent en `Authorization: Bearer`). Stateless côté serveur, mais : révocation difficile (il faut une liste noire ou des TTL très courts + refresh tokens), stockage côté client délicat (localStorage = exposé au XSS ; cookie = autant utiliser une session), et complexité de rotation.
- **Session serveur + cookie** : le serveur garde l'état de session, le client ne porte qu'un identifiant opaque dans un cookie. Révocation triviale (invalider la session), rien d'exploitable côté client si le cookie est `HttpOnly`.

Atlas est une web app classique (frontend Angular + backend même origine logique), sans besoin d'auth cross-service distribuée. La révocation simple et la sécurité priment sur le « stateless ».

## Décision

Authentification **par session serveur**, transportée par un **cookie `HttpOnly`** :

- À la connexion réussie, `AuthController` établit un `SecurityContext` (principal = `UserId`, autorité `ROLE_USER`) et le persiste en session via le `SecurityContextRepository`. Le conteneur émet un cookie `JSESSIONID`.
- **Attributs du cookie de session** : `HttpOnly` (inaccessible au JS → un XSS ne peut pas voler la session), `SameSite=Lax` (le cookie ne part pas sur les requêtes cross-site, ce qui bloque la majorité des CSRF), `Secure` en **prod** (cookie envoyé uniquement en HTTPS).
- **CSRF activé** (obligatoire dès qu'on s'authentifie par cookie). Pattern SPA : jeton dans un cookie `XSRF-TOKEN` lisible par le JS, renvoyé par Angular dans l'en-tête `X-XSRF-TOKEN`, validé côté serveur (`CookieCsrfTokenRepository` + `CsrfTokenRequestAttributeHandler`, cf. `SecurityConfig`).
- **Politique de session** : `IF_REQUIRED` (une session n'est créée qu'au login, pas pour les requêtes publiques).
- **Logout** : `POST /api/auth/logout` (géré par le filtre Spring Security) invalide la session et supprime le cookie → `204`.

Pour le MVP, la session vit dans le conteneur servlet (en mémoire). **Spring Session** (store JDBC/Redis pour partager les sessions entre instances) sera ajouté **si et quand** on passe à plusieurs instances — l'abstraction Spring Security ne changera pas, seul le store.

## Conséquences

**Positives**
- Révocation immédiate (invalider la session) — impossible aussi simplement avec un JWT.
- Cookie `HttpOnly` → la session n'est pas volable par XSS (contrairement à un JWT en localStorage).
- `SameSite=Lax` + CSRF token = double rempart contre le CSRF.
- Simplicité : pas de refresh token, pas de rotation de clés de signature.

**Négatives**
- État de session côté serveur (en mémoire au MVP) → pas scalable horizontalement tel quel. Mitigation prévue : Spring Session + store partagé au moment du scale.
- CSRF impose au frontend d'envoyer l'en-tête `X-XSRF-TOKEN` (câblage Angular, fait au Sprint 8).

**Neutres**
- `SameSite=Lax` (pas `Strict`) : un Player qui clique le magic link depuis son client mail arrive en navigation top-level, le cookie suit — `Strict` casserait certains de ces parcours. `Lax` est le bon compromis.
- Même origine logique frontend/backend en prod simplifie le CORS et les cookies ; en dev, CORS avec credentials est configuré explicitement.
