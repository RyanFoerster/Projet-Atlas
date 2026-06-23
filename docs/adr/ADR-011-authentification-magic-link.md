# ADR-011 : Authentification par magic link (email-only)

**Statut** : Accepté
**Date** : Sprint 1
**Décideur** : Ryan Foerster

## Contexte

Identity est le module-passerelle : tout Player commence par s'authentifier. Il faut choisir une méthode d'auth pour le MVP, en gardant la sobriété (pas de feature inutile) et la sécurité de base. Options envisagées :

- **Mot de passe** : classique, mais impose de gérer le hashing, la politique de complexité, le reset de mot de passe, le risque de fuite et de réutilisation. Beaucoup de surface pour peu de valeur sur une audience de niche.
- **OAuth social (Google, Apple…)** : pratique, mais dépendance à des tiers, complexité de configuration, et collecte de données chez un tiers — en tension avec le positionnement « respect du joueur ».
- **Magic link (email-only)** : l'utilisateur saisit son email, reçoit un lien à usage unique, clique, il est connecté. Pas de mot de passe à gérer.

L'audience (lifters sérieux) a forcément un email valide et consulté. Le premier login vaut inscription (signup implicite).

## Décision

L'authentification du MVP est un **magic link email-only** :

- L'utilisateur demande un lien en saisissant son email (`POST /api/auth/magic-link/request`). La réponse est **toujours** `202`, même si l'email est inconnu — on ne révèle jamais l'existence d'un compte (anti-énumération).
- Un **jeton UUID v7** est généré, associé à l'email, avec un **TTL de 15 minutes** et un **usage unique**. Stocké en base (`magic_links`).
- Le clic (`GET /api/auth/magic-link/consume?token=…`) consomme le jeton (l'entity refuse expiré/déjà-consommé) :
  - Player existant → login, session ouverte.
  - Email inconnu → l'email est marqué vérifié (en session), et l'utilisateur passe par une page d'**onboarding** pour choisir son nom (`POST /api/auth/complete-signup`) → création du compte + login. *(Signup flow « A ».)*

Volontairement **hors périmètre MVP** : 2FA, OAuth, avatars, mot de passe. À reconsidérer post-MVP si un besoin réel émerge.

### Threat model et mitigations

| Menace | Mitigation |
|---|---|
| Interception/fuite du lien | TTL court (15 min) + usage unique → fenêtre d'exploitation minimale. |
| Devinette du jeton | UUID v7 (122 bits aléatoires/temporels) → non devinable en pratique. |
| Énumération de comptes | Réponse identique (202) que l'email existe ou non. |
| Rejeu | Le jeton est invalidé à la première consommation (`consumedAt`). |
| Spam de liens | Rate limiting (max N/email/heure) — **différé post-MVP**, noté comme amélioration. |
| CSRF / vol de session | Cf. ADR-012 (cookie HttpOnly + SameSite + CSRF). |

## Conséquences

**Positives**
- Zéro gestion de mot de passe : pas de hashing, pas de reset, pas de fuite de credentials, pas de réutilisation.
- UX moderne et sobre, alignée sur le positionnement produit.
- Surface d'attaque réduite (un seul secret éphémère, pas de base de mots de passe).

**Négatives**
- Dépendance à la délivrabilité email : si l'email n'arrive pas, l'utilisateur est bloqué. Mitigé en dev par `LogOnlyEmailSender` (ADR-013) ; en prod, dépend de Resend.
- Login un peu plus lent (aller-retour email) qu'un mot de passe mémorisé. Acceptable pour la fréquence d'usage d'Atlas.

**Neutres**
- Le rate limiting est repoussé post-MVP : à implémenter avant l'ouverture publique.
- Le flow A (onboarding séparé) ajoute une étape mais sépare proprement « vérification de l'email » et « création du compte ».
