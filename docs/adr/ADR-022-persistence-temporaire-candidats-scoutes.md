# ADR-022 : Persistence temporaire des candidats scoutés (intégrité du recrutement)

**Statut** : Accepté
**Date** : Sprint 2
**Décideur** : Ryan Foerster

## Contexte

Le scouting génère un candidat ; `/recruit` crée l'athlète à partir de ce candidat. La question : d'où
vient le candidat au moment du recrutement ?

- **Renvoyer le candidat complet au client, puis le ré-accepter à `/recruit`** : simple, mais le client
  peut **forger** un candidat (un Prodigy à génétique maxée) → le système de rareté est trivialement
  contournable. En MVP solo l'enjeu est faible, mais ça touche l'intégrité, et un classement/compétition
  arrive plus tard.
- **Persister le candidat côté serveur** et ne manipuler qu'un id : le client ne peut plus rien forger.

## Décision

On persiste les candidats scoutés **temporairement** (option « état serveur », la version propre, pas la
dette technique) :

- `/scout` génère le candidat, l'enregistre comme **`ScoutedCandidate`** (id propre + TTL court, ~1h) et
  renvoie `{ candidateId, candidate }` (le candidat pour l'affichage, l'id pour la suite).
- `/recruit` prend **`{ candidateId }`** seul, reconstitue le candidat depuis la base, le **consomme**
  (usage unique) et crée l'athlète. `candidateId` inconnu/expiré/déjà consommé → erreur propre (404/409).
- **Calque mental = `MagicLink`** (identity) : objet à durée de vie courte, consommé une seule fois. Même
  pattern (`isExpired`, `canBeConsumed`, `consume`) — pas de nouveau pattern inventé.

**Stratégie TTL — ceinture + bretelles** :
- **Purge périodique** (`@Scheduled`) qui supprime les candidats expirés (`deleteExpiredBefore(now)`).
- **Re-check à la consommation** : `ScoutedCandidate.consume(now)` refuse un candidat expiré, même si la
  purge n'est pas encore passée. La purge horaire n'a donc pas besoin d'être strictement synchrone avec
  le TTL : recruter à 59 min 59 s passe, à 60 min 01 s renvoie une erreur propre via le re-check. Aucune
  race condition à craindre.

Stockage : table `scouted_candidates` (migration **V006** séparée — concept distinct, temporaire, vs les
athlètes persistants), le candidat complet en un blob **jsonb**. Index sur `expires_at` pour la purge.

## Conséquences

**Positives**
- **Intégrité** : impossible de forger un athlète — le serveur est la source de vérité.
- Pattern réutilisé (MagicLink), pas de complexité nouvelle.
- Bonus : trace des candidats scoutés (même refusés) exploitable plus tard pour des stats.

**Négatives**
- Une table + un job de purge à maintenir. Coût modeste.
- **Purge `@Scheduled` : limitation mono-instance assumée pour le MVP.** En cas de déploiement
  multi-instance futur, plusieurs instances lanceraient la purge en parallèle — prévoir alors un
  **`@SchedulerLock` (lib ShedLock)** ou une élection de leader pour éviter les purges concurrentes. Non
  bloquant aujourd'hui (mono-instance), tracé pour honnêteté.
- Écritures à chaque scout (même refusé) → volume de candidats temporaires ; borné par le TTL + la purge.

**Neutres**
- Le TTL (1h) est un paramètre ; ajustable selon l'usage réel.
