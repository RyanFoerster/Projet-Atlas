# Rétrospective — Sprint 0 (Bootstrap)

> Ce qui s'est bien passé, ce qui a coincé, ce qu'on change pour la suite. Honnête, pour servir réellement aux sprints suivants.

## Objectif du sprint (rappel)
Poser les fondations techniques d'Atlas : repo, structure modular monolith, build Maven, Spring Modulith, persistence testée, frontend Angular, CI. Zéro logique métier.

## Résultat
**Atteint.** Tous les critères de la Definition of Done sont remplis (build vert, `verify` Modulith, `/actuator/health`, Swagger, Flyway, app Angular zoneless avec appel API + CORS, CI GitHub Actions verte). Quelques versions de la doc initiale ont dû être corrigées (incompatibilités Boot 4.1) — tracées dans ADR-002/009.

---

## Ce qui s'est bien passé

- **Vérification systématique des versions sur Maven Central** avant d'écrire le `pom`. A évité de partir sur des versions incompatibles (springdoc 2.7 = Boot 3, Flyway 11, Testcontainers 1.x…). Le réflexe « ne jamais inventer une version » a payé immédiatement.
- **Exécution pas-à-pas avec gate de validation** entre chaque étape. Chaque palier était prouvé (build vert, endpoint qui répond, test qui passe) avant de continuer. Aucun « ça devrait marcher » non vérifié.
- **Séparation stricte refactor / feature dans les commits.** Le passage en monorepo a été fait en un commit `refactor:` pur (43 renames `R100`, zéro changement fonctionnel) puis un commit `feat:`. Historique git limpide.
- **Diagnostics menés jusqu'à la cause racine**, pas jusqu'au premier contournement : le bug Flyway (auto-config modularisée Boot 4) et le conflit de port 5432 (Postgres natif) ont été compris, pas masqués.
- **Décisions structurelles tracées en ADR** au fil de l'eau (ADR-009 versions, ADR-010 monorepo), pas après coup.
- **Capture des notes de mini-cours en continu** (MINI-COURSE-NOTES.md), ce qui a rendu la rédaction finale du mini-cours fidèle et complète.

## Ce qui a coincé

- **Spring Boot 4.1 est très récent** : plusieurs versions pinées dans la doc initiale étaient fausses ou incompatibles. Coût : du temps de vérification et deux ADR de correction. Mais détecté tôt.
- **La modularisation de l'auto-config Boot 4** a causé un bug silencieux (Flyway présent mais inactif). Symptôme déroutant : aucune erreur, juste un comportement absent. Résolu en passant au starter dédié.
- **Testcontainers 2.x a renommé ses artefacts** (`postgresql` → `testcontainers-postgresql`) : erreur « version missing » au build, le temps de comprendre le changement de coordonnées.
- **Conflit de port 5432** avec un PostgreSQL natif (EnterpriseDB) préexistant sur la machine : ~plusieurs allers-retours de diagnostic. Faux positif piégeux : le `pg_hba.conf` de l'image met `127.0.0.1` en `trust`, donc un test d'auth interne « passait » même avec un mauvais mot de passe.
- **Outillage à mettre à niveau en cours de route** : JDK 25 et Maven absents au départ ; Node 22.20 trop ancien pour Angular CLI 22 (bump vers Node 24). Friction d'environnement, pas de blocage de fond.

## Ce qu'on change pour la suite

- **Au sprint 1, écrire le test de détection de violation Modulith** (introduire une violation contrôlée, vérifier que `verify()` échoue, puis la retirer). Le `verify()` actuel sur modules vides ne prouve pas la détection. Noté dans `sprint-01-identity/NOTES.md`.
- **Réintroduire jqwik au sprint 4** (property-based tests du domaine) après vérification de sa compat avec JUnit Platform 6. Noté dans `sprint-04-fitness-fatigue-basic/NOTES.md`.
- **Réflexe « prouver que le problème est externe d'abord »** à systématiser sur tout bug d'intégration : tester le composant isolément (Testcontainers, accès direct) avant de fouiller la config applicative.
- **Vérifier la compat des libs hors écosystème Spring** (springdoc, Modulith, MapStruct) à chaque upgrade de Boot, et revérifier les versions figées dans `<properties>` (ADR-009).
- **Anticiper l'environnement de dev** en début de sprint (versions JDK/Node/outils) pour éviter les bumps en cours de route.

## Sur la collaboration Claude ↔ Ryan
- Le mode pas-à-pas avec validation explicite a bien fonctionné pour un sprint fondateur. À garder pour les décisions structurantes ; on pourra relâcher (enchaîner plusieurs étapes) sur les sprints plus mécaniques.
- Les ADR et le mini-cours produits en parallèle du code rendent le sprint réellement réutilisable (devblog, entretien, onboarding).

---

*Rétrospective Sprint 0 — maintenue par Ryan Foerster.*
