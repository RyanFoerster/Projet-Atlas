# Rétrospective — Sprint 1 (Identity & onboarding)

> Honnête, pour servir réellement aux sprints suivants. Le Sprint 1 a eu de vrais paliers de
> découverte — ils méritent d'être tracés.

## Objectif du sprint (rappel)
Premier module métier : Identity. Authentification par magic link (email-only), aggregate `User`,
session Spring Security, service email abstrait, frontend login/onboarding/home, et — surtout —
**poser le pattern DDD** que tous les modules suivants copieront.

## Résultat
**Atteint.** Flux magic link complet de bout en bout (vérifié curl + navigateur, dark/light),
123 tests backend + 14 frontend verts, seuil de couverture domaine ≥ 80 % *enforced*, test de
violation Modulith actif, CI verte sur l'ensemble. 8 ADR produits/complétés (011→018). Le pattern DDD
(value objects auto-validants, aggregate immutable, ports/adapters, mappers manuels) est posé et
documenté.

---

## Ce qui s'est bien passé

- **Discipline de vérification → bugs attrapés tôt (ou à temps).** Deux exemples décisifs : (1) avant de
  coder le callback Angular, un `curl` du flux `consume → /me` (cookies conservés) a confirmé que la
  session est créée *dans* le handler — évitant un débogage fantôme côté frontend. (2) Le check « vérifie
  dans le navigateur, pas juste que ça compile » a fait découvrir la friction CSRF *avant* de croire le
  sprint fini.
- **« Stop and signal » plutôt que workaround.** Sur MapStruct comme sur le CSRF, on s'est arrêté pour
  signaler la friction et décider proprement, au lieu de bricoler. Les deux ont donné un ADR (015, 018)
  et une meilleure solution.
- **Pattern DDD posé proprement et capturé au fil de l'eau** (`MINI-COURSE-NOTES.md`), ce qui a rendu le
  mini-cours fidèle.
- **Couverture comblée AVANT d'enforcer.** On a regardé le rapport, identifié le seul trou
  (`DomainException` à 50 %), l'a comblé, puis activé le seuil — et *prouvé* que le seuil s'arme
  (échoue à 0.99, passe à 0.80). Pas de garde-fou qui passe en silence.
- **ADR au fil de l'eau**, commités séparément du code.

## Ce qui a coincé

- **MapStruct a fait pivoter le plan.** Prévu pour le mapping domain↔JPA, il s'est révélé incompatible
  avec un aggregate riche (constructeur privé, accesseurs record-style, value objects). On a perdu un peu
  de temps à le diagnostiquer, puis pivoté vers des **mappers manuels** (ADR-015), MapStruct étant
  reporté à la frontière DTO web (S6) où il a sa place. Bonne décision, mais découverte en cours de route.
- **Friction CSRF découverte tardivement (à GATE 2).** Le premier POST de login partait en 403 : l'app
  fraîchement chargée n'avait pas encore le cookie `XSRF-TOKEN`. On l'a corrigée proprement (proxy
  same-origin + `APP_INITIALIZER`, ADR-018) — mais on aurait pu l'anticiper en concevant `SecurityConfig`.
  C'est le coût d'avoir pensé « backend d'abord » sans simuler le navigateur plus tôt.
- **Boot 4 a re-mordu.** La modularisation de l'autoconfigure, déjà rencontrée au Sprint 0 (Flyway), a
  frappé encore : `@AutoConfigureMockMvc` avait migré dans `spring-boot-webmvc-test`. Symptôme déroutant
  (classe introuvable), résolu en ajoutant le module dédié. Le pattern est maintenant un réflexe.
- **Coordination des numéros d'ADR entre deux flows parallèles.** Le module Identity (backend) et la
  mission design system (frontend) avançaient en parallèle ; tous deux voulaient « le prochain ADR ». On
  a géré (design = 016, shared OPEN = 017) mais de façon *réactive*, en se signalant le conflit au moment
  de mettre à jour l'index.
- **jqwik encore reporté.** Le property-based test d'`Email` a été fait en JUnit randomisé pur (seed fixe)
  en attendant l'introduction formelle de jqwik au Sprint 4.

## Ce qu'on change pour la suite

- **Simuler le navigateur plus tôt** pour les sujets cross-cutting (auth, cookies, CSRF). Une vérif
  « topologie réseau réelle » dès la conception de la sécurité aurait sorti la friction CSRF avant GATE 2.
- **Numérotation d'ADR : réserver les numéros à l'avance** quand plusieurs flows tournent en parallèle,
  plutôt que de résoudre le conflit à l'index.
- **Garder le réflexe « prouver le contrat backend en curl avant de coder le frontend »** — il a très bien
  marché, à systématiser.
- **MapStruct au Sprint 6** : l'introduire à la frontière DTO web (sa vraie place), pas avant.

## Sur la collaboration Claude ↔ Ryan
- Les **paliers de validation** (S1 puis enchaînement, GATEs 1 et 2 côté front) ont bien cadré un sprint
  dense, sans tout ralentir.
- Les **check curl demandés par Ryan** (session, puis re-vérif CSRF) ont eu un vrai ROI : ils ont
  transformé des « ça devrait marcher » en preuves, et attrapé la friction CSRF.
- Le **mode parallèle** (backend Identity + mission design system) a fonctionné parce que les deux étaient
  réellement indépendants ; la seule friction (numéros d'ADR) était bénigne.

---

*Rétrospective Sprint 1 — maintenue par Ryan Foerster.*
