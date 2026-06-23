# ADR-013 : Stratégie du service d'email (port + Resend prod + LogOnly dev)

**Statut** : Accepté
**Date** : Sprint 1
**Décideur** : Ryan Foerster

## Contexte

Le flow magic link (ADR-011) repose sur l'envoi d'un email transactionnel contenant le lien de connexion. Il faut décider :

- **par où** le domaine déclenche l'envoi, sans se coupler à un fournisseur ;
- **quel fournisseur** en production ;
- **comment** développer et tester sans envoyer de vrais emails.

Contraintes : le domaine doit rester pur (ADR-003) ; on ne veut ni clé d'API ni appel réseau en dev/test ; on veut pouvoir changer de fournisseur sans toucher au métier.

## Décision

### Port dans le domaine, adapters dans l'infrastructure

L'envoi est modélisé par un **port secondaire** `EmailSender` dans `identity/domain/port/`, exprimé en termes métier :

```java
void sendMagicLink(Email recipient, MagicLinkToken token);
```

Le domaine ne sait pas comment l'email part. Les implémentations (adapters) vivent dans `identity/infrastructure/email/` et sont sélectionnées **par profil Spring** :

| Profil | Bean actif | Comportement |
|---|---|---|
| dev / local / test (`!prod`) | `LogOnlyEmailSender` | N'envoie rien : journalise le lien complet (cliquable depuis les logs). |
| prod | `ResendEmailSender` | Envoie réellement via l'API **Resend**. |

La sélection se fait par `@Profile("!prod")` / `@Profile("prod")` → exactement un bean `EmailSender` par environnement.

### Resend en production

**Resend** est retenu comme fournisseur : free tier généreux, API moderne, intégration simple. L'adapter appelle son API REST via le `RestClient` de Spring (aucune dépendance supplémentaire), avec la clé d'API et l'expéditeur lus en configuration (`atlas.email.*`, injectés par variables d'environnement en prod).

### Construction du lien et des templates

La construction de l'URL du lien et du corps du message est centralisée dans `EmailTemplates`, partagé par les deux adapters. L'URL pointe vers le frontend : `{atlas.frontend.base-url}/auth/callback?token={token}`.

**Templates en texte simple pour le Sprint 1** : lien brut + message court (voix éditoriale Atlas). Les templates HTML riches (et leur prévisualisation via **Mailhog**) sont **différés au Sprint 9**, quand l'envoi sera peaufiné. Mailhog n'est donc pas câblé ce sprint.

## Conséquences

**Positives**
- Domaine découplé du fournisseur : changer Resend pour autre chose = un nouvel adapter, zéro impact métier.
- Dev et tests sans clé d'API ni réseau : le lien est dans les logs, le flow complet est jouable en local.
- Sélection par profil = pas de `if (env)` dispersés ; un seul bean actif, explicite.

**Négatives**
- `ResendEmailSender` n'est pas exercé en dev/test (gated `prod`) : son intégration réelle ne sera validée qu'au premier déploiement. Acceptable (code minimal, contrat Resend stable).
- Texte simple = email peu « léché » au Sprint 1. Assumé : la qualité visuelle de l'email viendra au Sprint 9 avec Mailhog et les templates HTML.

**Neutres**
- Le port `EmailSender` ne parle que de magic link aujourd'hui. Il s'étendra (emails de notification, etc.) au fil des besoins, en gardant le même principe port/adapters.
