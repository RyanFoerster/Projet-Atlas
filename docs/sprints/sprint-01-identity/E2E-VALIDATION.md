# Validation end-to-end — Sprint 1 (Identity & onboarding)

> Scénario de validation manuelle du flux d'authentification magic link, de bout en bout
> (frontend Angular ↔ backend Spring Boot ↔ PostgreSQL). Reproductible. Validé le 2026-06-23
> par curl (chemin same-origin proxifié) **et** dans le navigateur, dark et light.

## Prérequis

| Composant | Commande | Attendu |
|---|---|---|
| PostgreSQL | `docker compose up -d` | container `atlas-postgres` healthy |
| Backend | `cd backend && ./mvnw spring-boot:run` (profil `local`) | `GET :8080/actuator/health` → `200 {"status":"UP"}` |
| Frontend | `cd frontend && npm start` (Node 24) | `:4200` up, `proxy.conf.json` actif (`/api` → `:8080`) |

> En dev, aucun email n'est envoyé : `LogOnlyEmailSender` journalise le lien dans la console backend
> (`[DEV] … lien magique : http://localhost:4200/auth/callback?token=…`). Le récupérer là.

---

## Scénario A — Nouvel utilisateur (signup implicite, flow A)

| # | Action | Résultat attendu |
|---|---|---|
| A1 | Ouvrir `http://localhost:4200` | Redirection vers `/login` (layout Focus, titre Cormorant) |
| A2 | Saisir un email **inconnu**, cliquer « Recevoir le lien » | Bouton en `loading`, puis page `/login/sent` « Vérifie ta boîte mail » avec l'email |
| A3 | Récupérer le lien dans les logs backend, l'ouvrir | Page `/auth/callback` « Connexion en cours… » puis redirection `/onboarding` |
| A4 | Saisir un nom (≥ 2 caractères), « Continuer » | Compte créé + connecté → `/home` « Bonjour {nom} » |
| A5 | Cliquer « Se déconnecter » | Session détruite → retour `/login` |

**Vérifié (curl, chemin proxifié) :** `GET /me` (amorçage) → 401 + cookie `XSRF-TOKEN` ; `POST request` → 202 ;
`GET consume` → `200 {"newUser":true}` ; `POST complete-signup` → `200 {…displayName…}` ; `GET /me` → 200.

---

## Scénario B — Utilisateur existant (login direct)

| # | Action | Résultat attendu |
|---|---|---|
| B1 | `/login`, saisir un email **déjà inscrit**, « Recevoir le lien » | `/login/sent` |
| B2 | Ouvrir le lien des logs | `/auth/callback` → **directement** `/home` (pas d'onboarding) |
| B3 | Rafraîchir `/home` (F5) | Reste sur `/home` (réhydratation via `APP_INITIALIZER` → `GET /me` 200) |

**Vérifié (curl) :** `GET consume` → `200 {"newUser":false}` + session authentifiée créée dans le handler ;
`GET /me` direct → 200.

---

## Cas d'erreur (états async par composition)

| Cas | Action | Résultat attendu |
|---|---|---|
| Email invalide | `/login`, saisir `pas-un-email` | Erreur champ « Saisis une adresse email valide. » (pas d'appel réseau) |
| Lien expiré / déjà consommé | Ouvrir 2× le même lien, ou après 15 min | `/auth/callback` → « Lien inutilisable » + « Demander un nouveau lien » |
| Lien malformé | `/auth/callback?token=xyz` | « Lien inutilisable » (400 traduit en `InvalidMagicLinkException`) |
| Accès protégé non connecté | `/home` sans session | `authGuard` → redirection `/login` (`GET /me` 401 avalé) |
| CSRF manquant | POST sans en-tête `X-XSRF-TOKEN` | 403 (amorcé au boot par l'`APP_INITIALIZER`, donc transparent en usage normal) |

---

## Sécurité vérifiée

- Cookie de session `JSESSIONID` : **HttpOnly**, **SameSite=Lax** (observé dans les `Set-Cookie`).
- CSRF actif (double-submit cookie `XSRF-TOKEN` / en-tête `X-XSRF-TOKEN`, XSRF natif Angular).
- Anti-énumération : `POST /magic-link/request` répond **202** que l'email existe ou non.
- Topologie same-origin via proxy (ADR-018) → dev ≈ prod.

*Artefact maintenu par Ryan Foerster. À rejouer à chaque évolution du flux d'auth.*
