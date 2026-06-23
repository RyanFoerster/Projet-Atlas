# Rétrospective — Sprint 2 (Roster & génération procédurale)

*Date : 2026-06-23*

## Objectif du sprint (rappel)

Construire le module **roster** : l'écurie du Player, l'athlète miroir (créé à partir des vrais 1RM),
et le recrutement d'athlètes générés procéduralement. Tranche verticale complète (domaine → persistance
→ API → frontend). Et, en sous-texte, **prouver que le template DDD du Sprint 1 est réutilisable**.

## Résultat

- **Atteint, de bout en bout.** Domaine riche (TDD), persistance JSONB sur vrai PostgreSQL, 5 endpoints
  REST authentifiés, 4 pages frontend conformes au design system.
- **199 tests verts** (dont 48 nouveaux pour roster), JaCoCo `*.domain.*` ≥ 80 % forcé, isolation
  Spring Modulith verte.
- **4 ADRs** (019 aggregate/JSONB/kernel, 020 rareté par spécialisation, 021 génétique miroir hybride,
  022 candidats temporaires) — tous bâtis sur des décisions existantes, **zéro architecture nouvelle**.
- **3 paliers de validation** tenus : GATE 1 (modèle + algorithme calibré), GATE 2 (contrat REST prouvé
  au curl), GATE 3 (rendu visuel).

## Ce qui s'est bien passé

- **Réutilisation consciente d'un pattern temporel.** `ScoutedCandidate` est le calque de `MagicLink`
  (objet à TTL, consommé une fois). On n'a pas inventé un mécanisme — on a reconnu un pattern qu'on
  possédait. C'était l'objectif central du sprint, et il est atteint.
- **Calibration scientifique sourcée.** Les standards de force (ExRx, Nuckols/Stronger By Science) sont
  cités en JavaDoc et dans les ADRs. La distribution de rareté est vérifiée sur 10 000 tirages seedés
  (65.4/24.6/8.1/2.0 %, à ±0.5 %). Le réalisme est *observable* : dans l'UI, le boost hybride du miroir
  se lit directement sur les barres de force.
- **ArchUnit pour tester le template.** Le respect du squelette (4 couches, domaine pur, mappers manuels,
  events = records) est désormais une contrainte exécutable, pas une intention.
- **JSONB sans dépendance ni pollution du domaine.** Support natif Hibernate 7, prouvé par
  `jsonb_typeof = 'object'` et un round-trip exact.

## Ce qui a coincé

- **Tension `Athlete` entity vs aggregate root, repérée en lecture critique.** Le réflexe « tout est un
  aggregate » a failli créer un deuxième root. Tranché à temps (Roster seul root, Athlete entity interne,
  ADR-019) — mais ça montre que relire son propre plan *de façon adverse* est un réflexe à entretenir.
- **Kernel `shared` incomplet au démarrage.** `UserId` vivait dans identity ; le multi-module l'a
  révélé (Modulith aurait refusé l'import). Dette latente du Sprint 1, invisible tant qu'il n'y avait
  qu'un consommateur. Corrigée en S0 (promotion vers `shared`).
- **`/recruit` forgeable, identifié à temps.** Sans persistance serveur du candidat, un client pouvait
  forger un athlète. Corrigé par `ScoutedCandidate` (recrutement par id, ADR-022).
- **`LazyInitializationException` sur le chargement de l'aggregate.** La collection `athletes` (LAZY)
  lue hors transaction. Corrigé en rendant l'adapter de lecture `@Transactional`.
- **Friction d'outillage local.** `node@22` cassé sur la machine (lib manquante) → build frontend via
  `node@24`. Noté pour les prochains sprints.

## À surveiller (réévaluation future, pas une correction immédiate)

- **Variance perçue des Generic.** Observé au GATE 3 : un Generic tiré à 0.88–0.98 sur tous les axes est
  *visuellement fade* (cohérent avec la spec 0.88–1.05, mais peu différenciant). Pas un bug. **Signal
  d'usage** : si après ~10 scouts les Generic se ressemblent trop, élargir légèrement la base (p. ex.
  0.85–1.10) au Sprint 4+ pour gagner en variance perçue. À réévaluer avec du recul de jeu.
- **Layout App complet (sidebar §5.1).** Au Sprint 2, une simple top bar suffit. La sidebar viendra quand
  il y aura plusieurs sections (programmes, compétitions).
- **Stratégie d'évolution du format JSONB.** Tracée (ADR-019) mais repoussée au premier breaking change.
- **Purge `@Scheduled` mono-instance.** OK pour le MVP ; prévoir ShedLock si déploiement multi-instance
  (ADR-022).

## Ce qu'on change pour le Sprint 3

- **Relecture adverse systématique du plan** avant de coder un module : la tension Athlete a été évitée
  parce qu'on a relu *contre* nous-mêmes. En faire un rituel, pas un coup de chance.
- **Le frontend mérite son propre temps focalisé.** S5 a été dense ; isoler la construction des pages
  d'un gros bloc backend aide la qualité (design system non-négociable).
- **Ryan lance les serveurs dev lui-même** (backend + frontend) : meilleur contrôle pour itérer. Acté.

## Sur la collaboration Claude ↔ Ryan

- Le rythme **paliers + validation** (GATE 1/2/3) a très bien marché : chaque gate a attrapé un point réel
  (réconciliation du plafond 1.20/1.25, lisibilité du badge, friction des boutons scout).
- Les **décisions de modélisation co-affinées** (Athlete entity, état serveur pour le scout, casse des
  labels) plutôt que tranchées en solo : c'est exactement le mode prof-élève visé.
- À garder : Ryan tranche les choix de design produit (libellé « Phénomène », variante de badge), Claude
  propose et chiffre les options.
