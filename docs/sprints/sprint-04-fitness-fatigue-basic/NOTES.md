# Notes — Sprint 4 (Athletics, FitnessFatigueModel basique)

> Dossier créé au sprint 0 pour ne pas perdre des éléments reportés. À fusionner avec le `prompt.md` du sprint 4 quand il sera rédigé.

## Reports décidés lors de sprints précédents

### [Sprint 0] Introduction de jqwik (property-based testing) — À FAIRE ICI

jqwik a été **volontairement différé du sprint 0 au sprint 4**, car :

- jqwik (dernière version au sprint 0 : **1.10.1**) est bâti sur l'**ancien JUnit Platform 1.x**.
- Spring Boot 4.1 amène **JUnit Jupiter / Platform 6** (BOM : `junit-jupiter` 6.0.3).
- Il y a donc un **risque de conflit de compatibilité** entre jqwik et le JUnit Platform 6.
- jqwik ne sert qu'aux **property-based tests du domaine** (invariants mathématiques du `FitnessFatigueModel`, value objects), qui n'existent pas avant ce sprint.

**Action attendue au sprint 4 :**
1. Vérifier sur Maven Central la dernière version de `net.jqwik:jqwik` et sa dépendance `junit-platform`.
2. Confirmer la compatibilité avec le JUnit Platform amené par la version de Spring Boot alors en place (cf. `pom.xml`).
3. Si compatible : ajouter `net.jqwik:jqwik` (scope `test`) au `pom.xml` et écrire les premiers property-based tests du domaine athletics.
4. Si **non** compatible : signaler à Ryan, et choisir une alternative (attendre une version jqwik compatible, ou un autre framework de property-based testing supportant le Platform 6) avant de coder les tests.

Référence : ADR-002 (révision sprint 0), ADR-009.
