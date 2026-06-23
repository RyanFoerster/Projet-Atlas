# ADR-016 : Design system Atlas comme source de vérité visuelle

**Statut** : Accepté
**Date** : Sprint 1
**Décideur** : Ryan Foerster

## Contexte

Atlas a besoin d'une direction artistique cohérente et reconnaissable, applicable à toutes ses pages, pour ne pas tomber dans un design générique Tailwind par défaut qui crierait « side project ». L'identité visuelle est un asset produit : elle doit être identifiable au premier coup d'œil et tenir sur la durée, comme l'architecture backend.

Plusieurs directions visuelles ont été envisagées :

- sport-science analytique pur (dashboard froid type outil de data) ;
- gym vibe agressif (néon, contraste violent, « beast mode ») ;
- simulation premium (sombre, cinématique) ;
- mythologie iconique illustrative (Atlas figuratif, statues, illustrations) ;
- mythologie astronomique (cartes du ciel, constellations) ;
- mix **sport-science + antique sobre**.

Le **mix sport-science + antique sobre** a été retenu pour son alignement avec l'audience (lifters sérieux qui suivent la sport science) et le thème (Atlas, mythologie — sans devenir kitsch). Les directions illustratives/mythologiques figuratives ont été écartées : trop coûteuses à produire, vieillissant mal, et risquant le cliché (Trajan, laurels, colonnes). Une session de design dédiée a produit une spec complète et une page de preview vivante.

## Décision

Adoption du **design system Atlas** tel que documenté dans `docs/design-system/atlas-design-system.md` et illustré par `docs/design-system/preview.html` (preview composants) et `docs/design-system/tokens-preview.html` (preview palette & typographie).

Direction retenue : **sport-science + antique sobre, sans ornement décoratif**. Le dark mode est l'**identité signature** (riche, contrastée, mode par défaut) ; le light mode est la version claire neutre. La signature antique passe **uniquement** par la typographie (display serif lapidaire — Cormorant) et la palette (bronze tempéré comme seul accent de marque, sur fond minéral marbre/encre/ocre) : **aucun motif décoratif** (pas de méandres, laurels, colonnes, illustrations).

Le design system est la **source de vérité visuelle obligatoire** pour toute production frontend. Les règles d'usage (lecture préalable, interdiction du design improvisé, cohérence dark/light dès la création, accessibilité WCAG AA, procédure d'introduction d'un nouveau composant) sont formalisées dans **CLAUDE.md §6**.

## Conséquences

**Positives**
- Cohérence visuelle dès la première page du Sprint 1, sans refactor design ultérieur.
- Signature reconnaissable au premier coup d'œil, différenciation forte vs les trackers/jeux génériques.
- Vitesse accrue sur les pages futures : composants et patterns canoniques (Button, Card, Stat block, layouts) déjà disponibles.
- Démonstration de maturité produit en entretien (présence d'un vrai design system documenté, pas un thème improvisé).

**Négatives**
- Discipline à tenir : aucune classe Tailwind sortie du chapeau, tout passe par les tokens.
- Coût initial d'intégration technique (config Tailwind, fonts, toggle dark/light).
- Le design system devra évoluer au fil des sprints — discipline de mise à jour à maintenir.

**Neutres**
- Adoption de **Lucide** pour l'iconographie comme conséquence du système (icônes strictement fonctionnelles, jamais décoratives).
- Dark mode comme **défaut applicatif** (vs le light mode classique des outils enterprise).

## Notes d'implémentation (Sprint 1, mission d'intégration)

- Le frontend est en **Tailwind v4** (CSS-first) : la config `tailwind.config.js` (style v3) de la spec §9 est traduite en `@theme` + `@custom-variant dark` dans le CSS. **Les valeurs des tokens sont copiées telles quelles** depuis la spec — seul le mécanisme de déclaration change.
- Conformément à « dark = identité signature », les tokens dark sont déclarés sur `:root` (défaut, évite le flash de thème avant exécution du JS), les tokens light sur `[data-theme="light"]`.
- `lucide-angular` ne supporte pas encore Angular 22 (peerDeps plafonnées à 21.x) : pour cette mission, les rares icônes (toggle de thème) sont des **SVG Lucide inline**. Une approche structurée (`<atlas-icon>`) sera décidée quand le frontend Sprint 1 en aura besoin à plus grande échelle.
