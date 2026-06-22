# ADR-010 : Structure monorepo symétrique (backend/ + frontend/)

**Statut** : Accepté
**Date** : Sprint 0
**Décideur** : Ryan Foerster

## Contexte

Au sprint 0, le dépôt avait une structure **asymétrique** : le projet Maven occupait la racine (`pom.xml`, `src/` directement à la racine) tandis que l'application Angular vivait dans un sous-dossier `frontend/`. Cette asymétrie pose plusieurs problèmes :

- **Lisibilité** : un nouveau venu ne voit pas immédiatement qu'il s'agit d'un projet à deux applications ; le backend « se confond » avec la racine du dépôt.
- **Conventions mélangées** : les fichiers Maven (`pom.xml`, `target/`) et les conventions de la racine du dépôt (docs, scripts, CI) se chevauchent.
- **CI et déploiement** : une CI ou une config Dokploy qui pointe « la racine » pour le backend mais `frontend/` pour le front est fragile et difficile à raisonner.

Trois options étaient envisagées :

1. **Repos séparés** (un repo backend, un repo frontend) — découplage maximal mais lourd pour un projet solo : double CI, synchronisation des versions, double cérémonie de PR. Surdimensionné à ce stade.
2. **Statu quo asymétrique** (Maven à la racine, `frontend/` à côté) — zéro travail mais conserve les défauts ci-dessus.
3. **Monorepo symétrique** : un sous-dossier par application (`backend/`, `frontend/`), la racine ne contenant que ce qui est transverse (docs, scripts, orchestration, docker-compose).

## Décision

Le dépôt adopte une **structure monorepo symétrique** :

```
atlas/
├── backend/            # application Spring Boot — racine Maven (backend/pom.xml + Maven wrapper)
├── frontend/           # application Angular 22
├── docs/               # vision, glossaire, ADRs, sprints, learning
├── scripts/            # scripts de dev (dev-start.sh, db-reset.sh)
├── docker-compose.yml  # PostgreSQL local pour le dev
├── .gitignore          # règles transverses uniquement
└── CLAUDE.md
```

Précisions :

- **Maven wrapper** : `backend/` embarque `./mvnw` (pinné sur Maven 3.9.16). Toutes les commandes backend passent par le wrapper, sans dépendance à un Maven système — reproductibilité en CI et onboarding zéro friction (standard Spring Boot 2026).
- **`.gitignore` par niveau** : un `.gitignore` racine léger (OS, IDE, secrets transverses) + un `backend/.gitignore` (Java/Maven) + un `frontend/.gitignore` (Angular, généré par la CLI). Chaque sous-projet possède ses propres règles.
- **Scripts** : `scripts/*.sh` lancent `docker compose` depuis la racine et `./mvnw` depuis `backend/`.
- **docker-compose.yml** reste à la racine : c'est de l'orchestration de dev transverse (PostgreSQL local), pas un artefact backend.

## Conséquences

**Positives**
- Structure lisible et symétrique : chaque application a son dossier, la racine est dédiée au transverse.
- CI naturelle : un job par sous-projet avec `working-directory: backend` / `frontend`, caches distincts. Le Maven wrapper évite un `setup-maven`.
- Déploiement Dokploy clair : chaque application a son contexte de build (futur `backend/Dockerfile`, `frontend/Dockerfile`).
- Migration future vers des repos séparés possible sans réorganisation interne (chaque dossier est déjà autonome).
- **Historique git propre** : la migration a été faite en séparant strictement le commit `refactor:` (déplacement pur, sans changement fonctionnel) du commit `feat:` (la fonctionnalité en cours). Cette discipline — séparer les changements structurels des changements fonctionnels dans des commits distincts — est une bonne pratique que la structure encourage et qu'on applique systématiquement.

**Négatives**
- Petit coût de migration ponctuel (déplacement des fichiers, ajustement des scripts, de la CI à venir, de la doc).
- Les commandes backend nécessitent un `cd backend` (ou l'usage des scripts) — friction mineure, atténuée par le wrapper et les scripts.

**Neutres**
- La structure n'impose pas d'outil de build cross-projets, mais **laisse la porte ouverte** : si un jour on a besoin de builds incrémentaux orchestrés entre backend et frontend (cache de build partagé, invalidation fine), des outils comme le **Gradle build cache** ou **Bazel** s'intègrent naturellement sur une structure monorepo de ce type. Ce n'est pas prévu — juste rendu possible.
- Le choix monorepo vs multi-repo reste réversible à faible coût grâce à l'autonomie de chaque sous-dossier.

## Références

- ADR-002 — stack technique (Java/Spring backend, Angular frontend).
- CLAUDE.md §3 — structure physique du dépôt et structure de packages.
