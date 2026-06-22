# TODO de fin de Sprint 0

> Éléments à traiter au moment de clôturer le sprint (livrables documentaires de la Definition of Done + demandes spécifiques de Ryan accumulées en cours de route).

## Livrables DoD restants
- [ ] Devblog post `docs/blog/01-demarrage-atlas.md`
- [ ] Mini-cours `docs/learning/sprint-00-bootstrap.md`
- [ ] `RETROSPECTIVE.md` du sprint
- [ ] Récap pédagogique de fin de session

## Demandes spécifiques à intégrer dans le mini-cours `sprint-00-bootstrap.md`

### Section « Anatomie du pom.xml » (demandée par Ryan, étape 1)
Expliquer les concepts Maven moins évidents pour quelqu'un qui n'en a pas fait depuis quelques années :
- **`parent` vs `dependencyManagement` vs `dependencies`** : héritage (config + plugins + gestion de versions du parent) vs alignement de versions sans déclaration vs déclaration effective qui ajoute la dépendance au classpath.
- **`import` scope sur un BOM** : pourquoi importer un BOM (`<type>pom</type><scope>import</scope>`) diffère d'hériter d'un parent — permet d'agréger plusieurs BOMs là où on n'a qu'un seul parent.
- **`relativePath`** : à quoi il sert, et ce qui se passe quand il est vide (Maven ne cherche pas un parent sur le disque local, va directement au dépôt distant).
- **`-starter-*` vs artefact « nu »** : un starter est un méta-package d'opinion (agrège plusieurs dépendances cohérentes + auto-config) vs la lib sous-jacente seule.
- **Stratégie BOM-first (ADR-009)** appliquée concrètement dans notre `pom.xml` : ce qu'on hérite, ce qu'on fige dans `<properties>`, ce qu'on pinne explicitement.
