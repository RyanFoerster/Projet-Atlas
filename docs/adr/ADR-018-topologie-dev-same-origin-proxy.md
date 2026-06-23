# ADR-018 : Topologie de dev same-origin via proxy Angular

**Statut** : Accepté
**Date** : Sprint 1
**Décideur** : Ryan Foerster

## Contexte

Le frontend Angular (`localhost:4200`) consomme l'API du backend (`localhost:8080`). L'authentification repose sur un **cookie de session** + **CSRF par double-submit cookie** (ADR-011, ADR-012). Deux montages possibles en développement :

1. **Cross-origin** : le frontend appelle l'URL absolue `http://localhost:8080/...`, avec CORS (credentials) côté backend et lecture manuelle du cookie `XSRF-TOKEN` côté frontend.
2. **Same-origin via proxy** : le serveur de dev Angular proxifie `/api` vers `:8080`. Le navigateur ne voit qu'une seule origine (`:4200`).

Le montage cross-origin a révélé deux problèmes concrets (découverts en vérifiant le flux réel) :

- **Amorçage CSRF** : au premier chargement, l'app n'a jamais appelé le backend → pas de cookie `XSRF-TOKEN` → le tout premier POST (login) est rejeté en **403**.
- **Fragilité cross-origin** : lecture d'un cookie posé par une réponse cross-origin, subtilités `SameSite`, CORS credentials — fonctionne en `curl` mais fragile et non représentatif du navigateur.

Mais l'argument décisif n'est pas la robustesse : c'est la **fidélité dev↔prod**. En production, frontend et backend seront servis sur la **même origine**. Développer en cross-origin, c'est développer contre un environnement qui n'existera jamais en prod — on outille des problèmes (CORS, cookies tiers) qu'on n'aura pas, et on masque le comportement réel.

## Décision

On adopte la **topologie same-origin en dev, via le proxy du serveur de dev Angular** :

- `frontend/proxy.conf.json` renvoie `/api` vers `http://localhost:8080`, branché par `angular.json` (`serve.options.proxyConfig`). Le navigateur ne voit que `:4200`.
- Les appels API utilisent des **URL relatives** (`/api/auth/...`). La paramétrisation via `environment.ts` envisagée n'est **plus nécessaire** pour le cas standard same-origin ; elle ne redeviendrait utile que si le backend déménageait sur un autre domaine (non planifié).
- **CSRF/cookies natifs** : en same-origin, le support **XSRF natif d'Angular** (`provideHttpClient(withXsrfConfiguration(...))`) lit le cookie `XSRF-TOKEN` et pose l'en-tête `X-XSRF-TOKEN` automatiquement. L'**interceptor CSRF custom est supprimé** (il n'existait que pour contourner le cross-origin).
- **Amorçage + réhydratation** : un `APP_INITIALIZER` fait `GET /api/auth/me` au démarrage. Il pose le cookie `XSRF-TOKEN` (amorce le premier POST) **et** réhydrate la session si elle existe. Un **401 est un état normal** (utilisateur non connecté) : avalé silencieusement, `currentUser` reste `null`, le routing va vers `/login`. Aucun blocage du boot.

C'est le complément frontend des décisions backend **ADR-011** (auth magic link) et **ADR-012** (Spring Session + cookies HttpOnly + CSRF) : ces ADR décrivent le contrat serveur, celui-ci décrit comment le frontend s'y branche fidèlement.

## Conséquences

**Positives**
- **Dev ≈ prod** en topologie réseau : on développe et teste le vrai comportement same-origin (cookies first-party, CSRF natif).
- Plus de CORS ni de gestion manuelle de cookie côté frontend → moins de code, moins de pièges. Interceptor custom supprimé.
- Le premier POST (login) fonctionne grâce à l'amorçage ; le refresh de page réhydrate la session.

**Négatives**
- Le proxy est une pièce de config de dev à maintenir (`proxy.conf.json`).
- En prod, il faudra effectivement servir frontend et backend en same-origin (reverse proxy / même domaine) — c'est une contrainte de déploiement assumée (Dokploy, S9+).

**Neutres**
- La config CORS côté backend (ADR-012/SecurityConfig) devient superflue en dev same-origin ; on la conserve, inoffensive, au cas où un client cross-origin légitime apparaîtrait (outillage, tests).
