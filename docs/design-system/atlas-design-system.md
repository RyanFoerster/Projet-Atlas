# Atlas — Design System

> Jeu de coaching fitness type Football Manager, fondé sur la sport-science réelle.
> Direction : **sport-science antique, sobre**. L'antiquité par la lettre gravée et le pigment minéral — jamais par l'ornement.
> Dark = identité signature (riche, contrastée). Light = outil de travail (clair, neutre).

**Stack cible** : Angular + Tailwind CSS, composants 100 % custom (pas d'Angular Material). Polices Google Fonts self-hostables. WCAG AA minimum.

---

## 0. Principes directeurs

1. **Aucun ornement.** Typographie + couleur + grille + espace. Si une décoration ne porte pas d'information, elle n'existe pas.
2. **Un seul accent.** Le bronze tempéré est la seule couleur de marque. Tout le reste est minéral et désaturé.
3. **Le chiffre est roi.** Toute donnée numérique (poids, RPE, %, dates, deltas) est en monospace tabulaire. Les chiffres s'alignent, toujours.
4. **Display serif parcimonieux.** Cormorant n'apparaît que sur les titres de section et les moments épiques. ~90 % de l'interface est en sans-serif.
5. **Densité maîtrisée.** Beaucoup d'information, jamais de surcharge — la hiérarchie se fait par la taille, le poids et la couleur de texte, pas par des boîtes.
6. **Les deux modes sont des citoyens de première classe.** Tout token a sa valeur dark ET light.

---

## 1. Tokens de couleur

Déclarés en variables CSS sur `:root` (= dark, mode par défaut) et surchargés par `[data-theme="light"]`. Consommés par Tailwind en arbitrary values : `bg-[var(--surface-raised)]`, `text-[var(--text-secondary)]`, etc.

### 1.1 Déclaration CSS

```css
/* ===== DARK — défaut / signature ===== */
:root, [data-theme="dark"] {
  /* Surfaces */
  --surface-base:      #14130E;  /* hsl(50 18% 7%)   — fond app, ink chaud */
  --surface-sunken:    #0F0E0A;  /* hsl(48 20% 5%)   — zones dépressées, well */
  --surface-raised:    #1D1B15;  /* hsl(45 16% 10%)  — cards */
  --surface-raised-2:  #26241C;  /* hsl(48 15% 13%)  — popovers, surfaces sur card */
  --overlay:           rgba(8,7,5,0.66);

  /* Bordures (alpha sur blanc-marbre) */
  --border-subtle:     rgba(236,231,220,0.07);
  --border-default:    rgba(236,231,220,0.13);
  --border-strong:     rgba(236,231,220,0.22);

  /* Texte */
  --text-primary:      #ECE7DC;  /* hsl(41 30% 89%)  — titres, chiffres clés. AAA sur base */
  --text-secondary:    #BDB6A6;  /* hsl(42 15% 70%)  — corps. AAA sur base */
  --text-tertiary:     #8B8474;  /* hsl(42 9% 50%)   — méta, labels. AA */
  --text-disabled:     #5C5749;  /* hsl(44 12% 32%) */
  --text-inverted:     #14130E;  /* texte sur fond clair / sur accent */

  /* Accent — bronze tempéré (seule couleur de marque) */
  --accent:            #C7A05C;  /* hsl(38 49% 57%) */
  --accent-hover:      #D6B06B;  /* hsl(39 57% 63%) */
  --accent-active:     #AE8848;  /* hsl(38 41% 48%) */
  --accent-surface:    rgba(199,160,92,0.14);  /* fond teinté */
  --accent-text-on:    #14130E;  /* texte/icône sur aplat bronze */

  /* Sémantique — minérale */
  --success:           #8FA86A;  /* hsl(84 26% 54%)  — vert-de-gris */
  --success-surface:   rgba(143,168,106,0.14);
  --warning:           #D49A45;  /* hsl(36 62% 55%)  — ambre */
  --warning-surface:   rgba(212,154,69,0.14);
  --danger:            #C26049;  /* hsl(11 50% 52%)  — terre cuite */
  --danger-surface:    rgba(194,96,73,0.14);
  --info:              #7C93B8;  /* hsl(217 30% 60%) — lapis */
  --info-surface:      rgba(124,147,184,0.14);

  /* Data viz — 7 pigments distinguables */
  --dv-1: #C7A05C;  /* or       hsl(38 49% 57%) */
  --dv-2: #C97A57;  /* clay     hsl(18 51% 56%) */
  --dv-3: #8FA86A;  /* sauge    hsl(84 26% 54%) */
  --dv-4: #7C93B8;  /* lapis    hsl(217 30% 60%) */
  --dv-5: #A6789C;  /* porphyre hsl(313 21% 56%) */
  --dv-6: #5FA292;  /* stone    hsl(166 26% 50%) */
  --dv-7: #CDA64A;  /* ocre     hsl(42 57% 55%) */

  --shadow-sm: 0 1px 2px rgba(0,0,0,0.40);
  --shadow-md: 0 1px 2px rgba(0,0,0,0.40), 0 8px 24px rgba(0,0,0,0.35);
  --shadow-lg: 0 2px 4px rgba(0,0,0,0.45), 0 18px 48px rgba(0,0,0,0.45);
  --focus-ring: 0 0 0 2px var(--surface-base), 0 0 0 4px var(--accent);
}

/* ===== LIGHT — outil de travail / neutre ===== */
[data-theme="light"] {
  --surface-base:      #F4F1EA;  /* hsl(42 31% 94%)  — marbre clair */
  --surface-sunken:    #ECE7DC;  /* hsl(41 30% 89%) */
  --surface-raised:    #FBF9F4;  /* hsl(43 47% 97%) */
  --surface-raised-2:  #FFFFFF;  /* hsl(0 0% 100%) */
  --overlay:           rgba(34,31,24,0.40);

  --border-subtle:     rgba(20,19,14,0.08);
  --border-default:    rgba(20,19,14,0.14);
  --border-strong:     rgba(20,19,14,0.26);

  --text-primary:      #221F18;  /* hsl(42 17% 11%)  — AAA sur base */
  --text-secondary:    #4F4A3E;  /* hsl(42 12% 28%)  — AAA sur base */
  --text-tertiary:     #7A7464;  /* hsl(44 10% 44%)  — AA */
  --text-disabled:     #ABA493;  /* hsl(43 13% 62%) */
  --text-inverted:     #FBF9F4;

  --accent:            #8A6A2E;  /* hsl(39 50% 36%)  — bronze assombri, AA sur clair */
  --accent-hover:      #9C7B38;  /* hsl(40 47% 42%) */
  --accent-active:     #765925;  /* hsl(39 52% 30%) */
  --accent-surface:    rgba(138,106,46,0.12);
  --accent-text-on:    #FBF9F4;  /* texte clair sur aplat bronze foncé */

  --success:           #5E7A44;  /* hsl(91 28% 37%) */
  --success-surface:   rgba(94,122,68,0.12);
  --warning:           #A9772A;  /* hsl(36 60% 41%) */
  --warning-surface:   rgba(169,119,42,0.12);
  --danger:            #A8472F;  /* hsl(12 56% 42%) */
  --danger-surface:    rgba(168,71,47,0.12);
  --info:              #4F668C;  /* hsl(217 28% 43%) */
  --info-surface:      rgba(79,102,140,0.12);

  --dv-1:#8A6A2E; --dv-2:#A85C3D; --dv-3:#5E7A44; --dv-4:#4F668C;
  --dv-5:#82557A; --dv-6:#3F7E70; --dv-7:#A57F22;

  --shadow-sm: 0 1px 2px rgba(20,19,14,0.06);
  --shadow-md: 0 1px 2px rgba(20,19,14,0.06), 0 8px 24px rgba(20,19,14,0.08);
  --shadow-lg: 0 2px 4px rgba(20,19,14,0.08), 0 18px 48px rgba(20,19,14,0.12);
  --focus-ring: 0 0 0 2px var(--surface-base), 0 0 0 4px var(--accent);
}
```

### 1.2 Notes d'accessibilité

| Paire | Dark | Light |
|---|---|---|
| `text-primary` / `surface-base` | ~13:1 (AAA) | ~13:1 (AAA) |
| `text-secondary` / `surface-base` | ~7.5:1 (AAA) | ~8:1 (AAA) |
| `text-tertiary` / `surface-base` | ~4.6:1 (AA) | ~4.5:1 (AA) |
| `accent` / `surface-base` | ~6:1 (AA, AAA large) | ~5.3:1 (AA) |

- N'utilise **jamais** `text-tertiary` ou `text-disabled` pour du texte porteur de sens essentiel sans renfort (icône, position).
- Le bronze clair (`#C7A05C`) **ne passe pas** en texte fin sur surface claire → en light mode le texte d'accent utilise `--accent` (assombri). Les aplats bronze portent toujours `--accent-text-on`.
- Couleur jamais seule porteuse d'info (sémantique = couleur **+** icône **+** texte).

---

## 2. Système typographique

### 2.1 Familles (Google Fonts)

```html
<link href="https://fonts.googleapis.com/css2?family=Cormorant:wght@400;500;600&family=IBM+Plex+Sans:wght@400;500;600;700&family=IBM+Plex+Mono:wght@400;500;600&display=swap" rel="stylesheet">
```

| Rôle | Famille | Pourquoi |
|---|---|---|
| **Display** | **Cormorant** (serif) | Haute contraste, capitales tracées « marbre gravé ». Évoque l'inscription antique sans le cliché Trajan/Cinzel. Réservé aux titres de section + moments épiques. |
| **Body / UI** | **IBM Plex Sans** | Grotesque ingénieré, neutre, lisible en dense. Lit « sérieux scientifique » sans être tech-bro. Porte ~90 % de l'interface. |
| **Data** | **IBM Plex Mono** | Tabulaire, même squelette que Plex Sans → cohérence native. Tous les chiffres techniques. |

> Trois familles, point. Plex Sans + Plex Mono partageant la même super-famille, le système n'a en réalité que deux « voix » : la voix antique (Cormorant) et la voix scientifique (Plex).

**Tailwind**
```js
fontFamily: {
  display: ['Cormorant', 'serif'],
  sans:    ['"IBM Plex Sans"', 'system-ui', 'sans-serif'],
  mono:    ['"IBM Plex Mono"', 'ui-monospace', 'monospace'],
}
```
> Pour la data, ajouter `font-variant-numeric: tabular-nums;` (classe Tailwind `tabular-nums`) sur tout conteneur de chiffres.

### 2.2 Échelle

| Token | Famille | Taille / LH | Tracking | Transform | Usage |
|---|---|---|---|---|---|
| `display`  | Cormorant 500 | 54 / 1.05 | +0.5px | — | Moment épique (PR battu, écran de fin de cycle). 1× par écran max. |
| `h1`       | Cormorant 600 | 34 / 1.10 | +1px | UPPERCASE | Titre de section / nom d'entité héros. |
| `h2`       | Plex Sans 600 | 22 / 1.30 | -0.2px | — | Sous-titre, titre de bloc. |
| `h3`       | Plex Sans 600 | 18 / 1.40 | 0 | — | Titre de card. |
| `body-lg`  | Plex Sans 400 | 17 / 1.65 | 0 | — | Intro, paragraphe mis en avant. |
| `body`     | Plex Sans 400 | 15–16 / 1.65 | 0 | — | Corps de texte par défaut. |
| `body-sm`  | Plex Sans 400 | 13 / 1.50 | 0 | — | Helper text, légendes secondaires. |
| `caption`  | Plex Sans 500 | 11 / 1.40 | +1.1px (0.1em) | UPPERCASE | Labels, en-têtes de colonne, eyebrow. |
| `data`     | Plex Mono 500 | 14–15 / 1.40 | 0 | tabular | Poids, RPE, %, dates, deltas. |
| `data-lg`  | Plex Mono 500 | 28–32 / 1.10 | 0 | tabular | Grand chiffre d'un stat block. |

**Tailwind (extend.fontSize)**
```js
fontSize: {
  'display':  ['3.375rem', { lineHeight: '1.05', letterSpacing: '0.5px' }],
  'h1':       ['2.125rem', { lineHeight: '1.1',  letterSpacing: '1px' }],
  'h2':       ['1.375rem', { lineHeight: '1.3',  letterSpacing: '-0.2px' }],
  'h3':       ['1.125rem', { lineHeight: '1.4' }],
  'body-lg':  ['1.0625rem',{ lineHeight: '1.65' }],
  'body':     ['0.9375rem',{ lineHeight: '1.65' }],
  'body-sm':  ['0.8125rem',{ lineHeight: '1.5' }],
  'caption':  ['0.6875rem',{ lineHeight: '1.4', letterSpacing: '0.1em' }],
  'data':     ['0.875rem', { lineHeight: '1.4' }],
  'data-lg':  ['1.875rem', { lineHeight: '1.1' }],
}
```

### 2.3 Règle d'or d'emploi

- **Cormorant** → uniquement `display` et `h1`. Toujours avec un peu de tracking, souvent en capitales. Jamais en corps de texte, jamais en bouton, jamais sous 18px.
- **Plex Sans** → tout le reste : titres ≤ h2, corps, navigation, boutons, labels, formulaires.
- **Plex Mono** → **tout** ce qui est un chiffre mesuré (kg, lb, %, RPE, e1RM, Wilks, dates, durées, deltas), plus les labels techniques courts type tags de code. Jamais pour de la prose.

---

## 3. Système d'espacement

Base **4px**. Échelle géométrique douce, alignée sur Tailwind par défaut (`1`=4px). On nomme les paliers usuels pour le discours d'équipe ; en code on reste sur l'échelle Tailwind.

| Nom | px | Tailwind | Usage |
|---|---|---|---|
| `2xs` | 4  | `1`  | Gap icône/texte, padding interne d'un tag. |
| `xs`  | 8  | `2`  | Gap dans un groupe serré (chips, stat inline). |
| `sm`  | 12 | `3`  | Padding vertical d'input, gap de liste dense. |
| `md`  | 16 | `4`  | Padding standard de card compacte, gap de grille. |
| `lg`  | 24 | `6`  | Padding de card, gap entre blocs. |
| `xl`  | 32 | `8`  | Padding de section, marge entre groupes. |
| `2xl` | 48 | `12` | Respiration entre grandes sections. |
| `3xl` | 64 | `16` | Marge haut/bas de page, séparateurs majeurs. |
| `4xl` | 96 | `24` | Padding bas de page, écrans focus. |

**Rayons**

| Token | px | Tailwind | Usage |
|---|---|---|---|
| `radius-xs` | 6  | `rounded-md`  | Tags, badges, inputs petits. |
| `radius-sm` | 8  | `rounded-lg`  | Boutons, inputs. |
| `radius-md` | 12 | `rounded-xl`  | Cards. |
| `radius-lg` | 16 | `rounded-2xl` | Cards héros, modales, panneaux. |
| `radius-full` | 9999 | `rounded-full` | Avatars, pastilles d'état, switch. |

> Pas de rayons agressifs. Atlas n'est ni rond-mignon ni dur-brutaliste : 8–16px partout.

---

## 4. Composants de base

Anatomie, états, et classes Tailwind concrètes. Les classes supposent la config tokens de la §1/§2. `focus-visible` porte toujours `outline-none` + `shadow-[var(--focus-ring)]`.

### 4.1 Button

Hauteurs : `sm`=32px, `md`=40px, `lg`=48px. Police `font-sans font-medium`, jamais Cormorant. Tap target ≥ 44px sur mobile (md/lg).

**Variantes**

```html
<!-- Primary : aplat bronze. 1 seul par zone d'action. -->
<button class="inline-flex items-center justify-center gap-2 h-10 px-4 rounded-lg
  font-sans font-semibold text-[0.9375rem]
  bg-[var(--accent)] text-[var(--accent-text-on)]
  hover:bg-[var(--accent-hover)] active:bg-[var(--accent-active)]
  disabled:opacity-40 disabled:pointer-events-none
  focus-visible:outline-none focus-visible:shadow-[var(--focus-ring)]
  transition-colors duration-150">
  Lancer le cycle
</button>

<!-- Secondary : surface + bordure -->
<button class="… bg-[var(--surface-raised)] text-[var(--text-primary)]
  border border-[var(--border-default)]
  hover:border-[var(--border-strong)] hover:bg-[var(--surface-raised-2)]
  active:bg-[var(--surface-sunken)] …">
  Annuler
</button>

<!-- Ghost : transparent, pour barres d'outils -->
<button class="… bg-transparent text-[var(--text-secondary)]
  hover:bg-[var(--accent-surface)] hover:text-[var(--text-primary)] …">
  Détails
</button>

<!-- Destructive : aplat terre cuite, action irréversible uniquement -->
<button class="… bg-[var(--danger)] text-[var(--text-inverted)]
  hover:brightness-110 active:brightness-95 …">
  Retirer l'athlète
</button>
```

**États**
- `hover` : variation de fond/bordure (cf. ci-dessus), `transition-colors duration-150`.
- `active` : teinte plus sombre / surface sunken.
- `disabled` : `opacity-40 pointer-events-none`.
- `loading` : `aria-busy="true"`, texte remplacé par un spinner `<svg class="animate-spin h-4 w-4">`, largeur figée pour éviter le saut.
- `focus-visible` : `shadow-[var(--focus-ring)]` (anneau bronze à 2px de la surface).

### 4.2 Input

```html
<div class="flex flex-col gap-1.5">
  <label class="font-sans text-caption uppercase tracking-[0.1em] text-[var(--text-tertiary)]">
    Poids de barre
  </label>
  <div class="relative">
    <input type="number"
      class="w-full h-10 px-3 rounded-lg
        bg-[var(--surface-sunken)] text-[var(--text-primary)]
        font-mono tabular-nums text-[0.9375rem]
        border border-[var(--border-default)]
        placeholder:text-[var(--text-disabled)]
        hover:border-[var(--border-strong)]
        focus:outline-none focus:border-[var(--accent)] focus:shadow-[var(--focus-ring)]
        disabled:opacity-40
        transition-colors duration-150"
      placeholder="0">
    <span class="absolute right-3 top-1/2 -translate-y-1/2 font-mono text-body-sm text-[var(--text-tertiary)]">kg</span>
  </div>
  <p class="font-sans text-body-sm text-[var(--text-tertiary)]">Charge de travail, hors échauffement.</p>
</div>
```

**État erreur** : bordure `border-[var(--danger)]`, message en `text-[var(--danger)]` avec icône `lucide-alert-circle` 16px, remplace le helper text.
**Champs numériques** : toujours `font-mono tabular-nums`. **Champs texte** (nom, e-mail) : `font-sans`.
**Focus** : bordure bronze + anneau. Le focus est **toujours** visible (jamais `outline-none` seul).

### 4.3 Card

```html
<!-- basic -->
<div class="rounded-xl bg-[var(--surface-raised)] border border-[var(--border-default)] p-6">…</div>

<!-- interactive (cliquable) -->
<button class="text-left rounded-xl bg-[var(--surface-raised)] border border-[var(--border-default)] p-6
  hover:border-[var(--border-strong)] hover:shadow-[var(--shadow-md)]
  active:translate-y-px
  focus-visible:outline-none focus-visible:shadow-[var(--focus-ring)]
  transition duration-150">…</button>

<!-- highlighted (mise en avant : programme actif, alerte) -->
<div class="rounded-xl bg-[var(--surface-raised)] p-6
  border border-[var(--accent)]/40
  shadow-[inset_0_0_0_1px_var(--accent-surface)]">…</div>
```
Padding standard `p-6` (24px) ; `p-4` (16px) en densité élevée. Rayon `rounded-xl` (12px), `rounded-2xl` pour les cards héros.

### 4.4 Badge / Tag

Hauteur 22px, `text-caption uppercase tracking-[0.08em]`, `rounded-md px-2 py-0.5`, `inline-flex items-center gap-1`. Toujours surface teintée + texte de la même famille sémantique.

```html
<!-- PR / succès --><span class="… bg-[var(--success-surface)] text-[var(--success)]">PR</span>
<!-- Deload / warning --><span class="… bg-[var(--warning-surface)] text-[var(--warning)]">Deload</span>
<!-- Peak / accent --><span class="… bg-[var(--accent-surface)] text-[var(--accent)]">Peak</span>
<!-- Blessure / danger --><span class="… bg-[var(--danger-surface)] text-[var(--danger)]">Blessé</span>
<!-- Neutre / méta --><span class="… bg-[var(--surface-raised-2)] text-[var(--text-tertiary)] border border-[var(--border-default)]">Hors-saison</span>
```

### 4.5 Stat / Metric block

L'élément central d'Atlas. Anatomie : **label** (caption) → **valeur** (data-lg, mono) → **delta** (data, sémantique).

```html
<div class="rounded-xl bg-[var(--surface-base)] border border-[var(--border-subtle)] p-5">
  <div class="font-sans text-caption uppercase tracking-[0.08em] text-[var(--text-tertiary)] mb-3">
    1RM Bench
  </div>
  <div class="font-mono tabular-nums text-data-lg text-[var(--text-primary)]">
    120<span class="text-[1.125rem] text-[var(--text-tertiary)]">kg</span>
  </div>
  <div class="mt-2 font-mono tabular-nums text-data text-[var(--success)] flex items-center gap-1">
    <!-- lucide-trending-up 14px -->+5&nbsp;kg vs cycle précédent
  </div>
</div>
```
**Deltas** : positif → `text-[var(--success)]` + `trending-up` ; négatif → `text-[var(--danger)]` + `trending-down` ; neutre → `text-[var(--text-tertiary)]` + `minus`. L'unité (`kg`, `%`) est toujours en `text-tertiary`, plus petite que le nombre.

### 4.6 Table

Densité élevée, lisible. En-têtes en `caption`, corps en `body-sm`, **chiffres en mono tabulaire et alignés à droite**.

```html
<table class="w-full text-left border-collapse">
  <thead>
    <tr class="border-b border-[var(--border-default)]">
      <th class="py-2.5 px-3 font-sans text-caption uppercase tracking-[0.08em] text-[var(--text-tertiary)] font-medium">Date</th>
      <th class="… ">Exercice</th>
      <th class="… text-right">Sets×Reps</th>
      <th class="… text-right">Poids</th>
      <th class="… text-right">RPE</th>
    </tr>
  </thead>
  <tbody>
    <tr class="border-b border-[var(--border-subtle)] hover:bg-[var(--accent-surface)] transition-colors duration-100">
      <td class="py-3 px-3 font-mono text-body-sm text-[var(--text-tertiary)]">12 jun</td>
      <td class="py-3 px-3 font-sans text-body text-[var(--text-primary)]">Développé couché</td>
      <td class="py-3 px-3 font-mono tabular-nums text-data text-[var(--text-secondary)] text-right">5×3</td>
      <td class="py-3 px-3 font-mono tabular-nums text-data text-[var(--text-primary)] text-right">110&nbsp;kg</td>
      <td class="py-3 px-3 font-mono tabular-nums text-data text-[var(--text-secondary)] text-right">8.5</td>
    </tr>
  </tbody>
</table>
```
Lignes : `py-3` (confortable) ou `py-2` (compact). Hover de ligne en `accent-surface`. Zébrures **proscrites** — on sépare par bordures `border-subtle`. Colonnes numériques alignées à droite ; colonnes texte à gauche.

### 4.7 Navigation

Modèle **sidebar gauche + top bar** (Linear / Football Manager).

**Sidebar** (largeur 240px desktop, collapsible en rail 64px, drawer en mobile) :
```html
<aside class="w-60 shrink-0 h-screen sticky top-0 flex flex-col
  bg-[var(--surface-sunken)] border-r border-[var(--border-default)]">
  <!-- Wordmark -->
  <div class="h-14 flex items-center px-5 border-b border-[var(--border-subtle)]">
    <span class="font-display font-semibold text-[1.25rem] uppercase tracking-[0.22em] text-[var(--text-primary)]">Atlas</span>
  </div>
  <!-- Nav items -->
  <nav class="flex-1 p-3 flex flex-col gap-0.5">
    <!-- item actif -->
    <a class="flex items-center gap-3 h-9 px-3 rounded-lg font-sans text-body
       bg-[var(--accent-surface)] text-[var(--text-primary)]">
      <!-- lucide icon 18px --> Écurie
    </a>
    <!-- item inactif -->
    <a class="flex items-center gap-3 h-9 px-3 rounded-lg font-sans text-body
       text-[var(--text-secondary)]
       hover:bg-[var(--surface-raised)] hover:text-[var(--text-primary)]
       transition-colors duration-100">
      <!-- icon --> Programmes
    </a>
  </nav>
</aside>
```
- L'item actif porte `accent-surface` + texte primaire + (optionnel) un trait bronze 2px à gauche.
- **Top bar** (hauteur 56px) : contexte (fil d'Ariane / nom d'écran à gauche), actions globales + recherche + toggle thème + avatar à droite. `bg-[var(--surface-base)]/88 backdrop-blur border-b border-[var(--border-subtle)] sticky top-0`.

### 4.8 RarityBadge (module roster)

Variante de Badge/Tag (4.4) pour la **rareté d'un athlète**. Quatre paliers (`GENERIC`, `PROMISING`,
`SPECIALIST`, `PRODIGY`). **Principe non-négociable** : palette **minérale**, hiérarchie par **graisse +
saturation**, jamais par saut de teinte (pas de gris/bleu/violet/or façon jeu vidéo). Les trois premiers
paliers sont des surfaces teintées discrètes ; seul le palier rare est un **aplat plein** — « la beauté de
l'écart, pas de la décoration ». Comme tout badge : `h-[22px] text-caption uppercase tracking-[0.08em]
rounded-md px-2 py-0.5 inline-flex items-center gap-1`.

| Palier | Libellé FR | Fond | Texte | Bordure |
|---|---|---|---|---|
| GENERIC | Générique | `--surface-raised-2` | `--text-tertiary` | `--border-default` |
| PROMISING | Prometteur | `rgba(205,166,74,0.14)` (ocre/14%) | `--dv-7` `#CDA64A` | — |
| SPECIALIST | Spécialiste | `--accent-surface` | `--accent` `#C7A05C` | — |
| PRODIGY | **Phénomène** | `--accent` `#C7A05C` **plein** | `--accent-text-on` `#14130E` | — |

- **Libellé `PRODIGY` → « Phénomène »** (pas « Prodige », jugé enfantin) : registre sport-outlier, mature.
  Le mapping enum→libellé est côté front (le domaine garde `Rarity.PRODIGY`).
- **Contraste (vérifié, WCAG)** : seul l'aplat plein du Phénomène porte du texte — `#14130E` sur `#C7A05C`
  = **7.6:1**, AA **et AAA** pour texte normal. Les trois autres paliers utilisent du texte teinté sur
  surface sombre/neutre (≥ AA). Couleur jamais seule porteuse de sens : le libellé texte est toujours présent.
- **Cohérence métier** : la distribution est 65/25/8/2 % — le Phénomène est l'exception par construction,
  son unique aplat plein le rend reconnaissable instantanément dans une grille dense.

```html
<!-- GENERIC -->   <span class="… bg-[var(--surface-raised-2)] text-[var(--text-tertiary)] border border-[var(--border-default)]">Générique</span>
<!-- PROMISING --> <span class="… bg-[rgba(205,166,74,0.14)] text-[var(--dv-7)]">Prometteur</span>
<!-- SPECIALIST -->_<span class="… bg-[var(--accent-surface)] text-[var(--accent)]">Spécialiste</span>
<!-- PRODIGY -->   <span class="… bg-[var(--accent)] text-[var(--accent-text-on)] font-semibold">Phénomène</span>
```

### 4.9 StatBlock — barre de valeur normalisée (module roster)

Brique de la **visualisation génétique**. À distinguer du Stat/Metric block (4.5, gros chiffre + delta) :
ici, une **ligne-barre horizontale** pour une valeur dans une plage connue, avec **repère de baseline à 1.0**
(la « moyenne » génétique) — ce repère transforme un chiffre abstrait en information visuelle immédiate.

- **Anatomie** (ligne) : `label` (gauche) · `track` (barre, `--surface-sunken` + `border-subtle`) avec
  `fill` proportionnel (`--accent`) et un **tick baseline 1.0** · `value` (Plex Mono tabular, droite,
  `--text-primary` ; unité `--text-tertiary`).
- **Plage** : normalisée sur la plage du système (force 0.80–1.25, hypertrophie 0.85–1.30). Le fill = position
  dans la plage ; au-delà de 1.0 le fill dépasse visiblement le tick.
- **Couleur** : fill **neutre bronze** (`--accent`), **jamais** vert/rouge sémantique — réservé aux deltas
  dynamiques ; la génétique est un potentiel statique.
- **État `highlight`** (axe spécialisé) : valeur en `--text-primary` **semibold** + point bronze ; le reste
  en `--text-secondary`.
- **Label** : caption uppercase par défaut. **Si l'uppercase sonne « crié »** sur les noms de groupes/patterns,
  basculer en *regular case* + `--text-tertiary` semibold (à trancher en intégration).
- **Regroupement** : les StatBlock s'empilent sous un en-tête de groupe (caption) — « Hypertrophie » (par
  MuscleGroup), « Force » (par MovementPattern), puis « Récupération / Fibres / Sensibilité ».

### 4.10 AthleteCard (module roster)

Compose Card (4.3) + RarityBadge (4.8) + StatBlock (4.9).

- **Anatomie** : en-tête `nom` (`font-display`) + `RarityBadge` (haut-droite) ; ligne méta `âge · poids`
  (mono, `--text-tertiary`) ; si miroir, tag discret `Miroir` (`accent-surface` + icône).
- **Variantes** :
  - `summary` (grille roster) : nom + badge + méta + tag miroir. `p-4`, **carte interactive** (variante
    cliquable de Card) → `/roster/athletes/:id`.
  - `detailed` (résultat de scout) : `summary` + **la génétique COMPLÈTE par défaut** (tous les axes via
    StatBlock, regroupés) + actions `Recruter` / `Refuser`. `p-6`. Un joueur sérieux doit voir le profil
    entier pour décider — y compris les points faibles d'un Phénomène déséquilibré. Si trop dense, prévoir
    un toggle « voir le détail », mais **le défaut reste tout visible** (ne jamais réduire à un top-N).
- **États** : `default` / `hover` (bordure `border-strong` + `shadow-md`) / `focus-visible` (focus-ring) /
  `loading` (squelette pendant le scout).
- **Comportement** : `summary` navigue ; `detailed` émet `recruit`/`refuse` (pas de navigation propre).

### 4.11 Select (primitive de formulaire)

`<select>` natif **stylé comme l'Input (§4.2)** : clavier et accessibilité gratuits, zéro lib (cohérent
« pas d'Angular Material »). On masque le chrome natif (`appearance-none`) et on superpose un chevron Lucide.

```html
<div class="relative">
  <select class="w-full h-10 pl-3 pr-9 rounded-lg appearance-none
    bg-[var(--surface-sunken)] text-[var(--text-primary)] font-sans text-[0.9375rem]
    border border-[var(--border-default)]
    hover:border-[var(--border-strong)]
    focus:outline-none focus:border-[var(--accent)] focus:shadow-[var(--focus-ring)]
    disabled:opacity-40 transition-colors duration-150">
    <option>Squat</option>
  </select>
  <!-- lucide-chevron-down 16px -->
  <span class="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-tertiary)]">▾</span>
</div>
```
- **Texte** `font-sans` (libellés, pas de chiffres) ; hauteur/bordure/focus **identiques à l'Input**.
- **État erreur** : même langage que l'Input (bordure `--danger` + message).
- **Cross-browser** : `appearance-none` + chevron overlay diverge entre Firefox/Chrome/Safari → **vérifier le
  rendu sur les trois** (la flèche native peut réapparaître si `appearance` incomplet).

### 4.12 SegmentedControl (primitive)

Choix **mutuellement exclusif** compact (2–3 segments). Plus « outil » qu'un groupe de radios, plus explicite
qu'un dropdown — le choix devient un acte conscient. Usage canonique : type d'exercice **Composé / Accessoire**.

```html
<div role="radiogroup" class="inline-flex p-0.5 rounded-lg bg-[var(--surface-sunken)] border border-[var(--border-default)]">
  <button role="radio" aria-checked="true"
    class="h-8 px-3 rounded-md font-sans text-body-sm
      bg-[var(--surface-raised)] text-[var(--text-primary)] shadow-[var(--shadow-sm)]">Composé</button>
  <button role="radio" aria-checked="false"
    class="h-8 px-3 rounded-md font-sans text-body-sm
      text-[var(--text-tertiary)] hover:text-[var(--text-primary)] transition-colors duration-100">Accessoire</button>
</div>
```
- **États** : default / hover / **selected** (fond `surface-raised` + ombre + texte primaire) / focus-visible
  (focus-ring sur le segment) / disabled.
- **A11y** : `role="radiogroup"` + `role="radio"` + `aria-checked` ; flèches gauche/droite naviguent ; le
  segment actif a fond **et** poids (couleur jamais seule).

### 4.13 ExerciseSetRow (module personaltraining)

Sous-ligne d'une série dans le logger. Grille `reps · poids · rpe · supprimer`, **inputs numériques mono**
(Input §4.2). Densité Football Manager.

- **Champs** : `reps` **requis** (1–100) ; `poids` **optionnel** — *vide = poids de corps* (gainage, traction) —
  suffixe `kg` ; `rpe` **optionnel**, **1.0–10.0 par incréments de 0.5** (ex. 7.5, 8.5 — standard powerlifting,
  aligné sur le VO `RPE` du domaine).
- **États** : idle / focus (par champ) / error (bordure + message sous la grille, langage Input) / disabled (submit).
- **Clavier (attendu des lifters)** : **Tab** parcourt reps→poids→rpe ; **Enter** sur la dernière série **ajoute
  une série en dupliquant la dernière** (reps + poids gardés, **RPE vidé**) et place le focus sur ses reps.
  Supprimer une série la retire (jamais sous 1 série/exercice).

### 4.14 ExerciseLogRow (module personaltraining)

Bloc d'un exercice dans le logger. `Card p-4` avec **liseré gauche 2px** qui encode la catégorie — il rend la
nature de chaque exercice **scannable d'un coup d'œil** dans une longue séance (langage FM).

- **Anatomie** : en-tête = `SegmentedControl` (Composé/Accessoire) + `Input` nom (texte) + `Select`
  (MovementPattern **ou** BodyRegion selon le segment). Corps = mini en-tête de colonnes (caption
  `reps · poids · rpe`) + liste de `ExerciseSetRow`. Pied = `+ ajouter une série` (lien discret) +
  `supprimer l'exercice` (icône, à droite).
- **Variantes** (pilotées par le segment) :
  - `compound` : liseré **bronze** (`--accent`), Select = **MovementPattern** (Squat, Développé couché…) → API `pattern` ;
  - `accessory` : liseré **neutre** (`--border-strong`), Select = **BodyRegion** (Biceps, Gainage…) → API `region`.
- **Comportement** : changer de segment **swap le Select et réinitialise sa valeur** (1er item) ; ≥1 série
  toujours présente ; supprimer l'exercice retire le bloc (≥1 exercice par séance).
- **États** : default / champ en error / disabled (submit) / **invalid** au submit (nom vide ou 0 série → bordure
  subtile `--danger`). Le `pattern` XOR `region` est garanti par construction (le segment détermine lequel est envoyé).

### 4.15 WorkoutSessionCard (module personaltraining)

Carte d'une séance dans l'historique chronologique (`/training`). `Card` **interactive** → `/training/sessions/:id`.

- **Anatomie** : ligne 1 = **date relative** (`font-display`, « il y a 2 jours ») + date absolue discrète (mono,
  `--text-tertiary`) ; durée (`75 min`, mono) à droite si présente. Ligne 2 = **patterns couverts** en `Badge`
  neutres — les **accessoires ne sont pas listés** (cohérent avec `patternsCovered()`, ADR-026). Ligne 3 (méta
  mono `--text-tertiary`) = `N exercices · X séries · Y reps`.
- **États** : default / hover (Card interactive) / focus-visible.
- **Empty state** (historique vide) : **par composition** (layout + voix « Tu n'as pas encore loggé de séance. »),
  pas de nouveau composant (doctrine async-states).

### 4.16 ConditionGauge — indicateur de Forme (module athletics)

Affiche l'état de forme **dynamique** (Fitness-Fatigue de Banister) d'un athlète sur sa fiche. À distinguer du
`StatBlock` (§4.9, génétique **statique**, fill bronze neutre) : ici l'état est dynamique, donc les **couleurs
sémantiques minérales sont légitimes** — exactement l'usage que la §4.9 réserve aux « deltas dynamiques ».

- **Anatomie** :
  - **En-tête** : label `FORME` (caption) + grand **indice 0–100** (Plex Mono tabular, `--text-primary`) ;
    **pill d'état** (`Cuit`/`Frais`/`Affûté`) à droite.
  - **Barre de forme** : track `--surface-sunken` + `border-subtle` (hauteur `8px`), fill = couleur d'état,
    largeur = indice %. **Tick neutre à 50** (`--text-tertiary`, idiome baseline du StatBlock) — sous 50 =
    sur-fatigué, au-dessus = affûté.
  - **Détail Acquis / Fatigue** : deux **mini-barres plus fines** (hauteur `4px`, `rounded-full`, sous un
    libellé), normalisées sur `max(fitness, fatigue)` — l'échelle interne NORM n'est pas lisible en absolu, on
    montre la **proportion relative**. `Acquis` (fitness) en `--accent` (bronze = capital), `Fatigue` en
    `--warning` (ambre = fatigue résiduelle). **Visuellement distinctes de la grande barre** (plus fines).
- **États / couleurs** (minéral, jamais de feu tricolore) :
  - `Affûté` → `--success` (vert-de-gris) ; pill `--success-surface` / `--success`.
  - `Frais` → neutre ; pill `--surface-raised-2` / `--text-secondary` / `border --border-default`.
  - `Cuit` → `--warning` (ambre). **Pas `--danger`** : la sur-fatigue est un état d'entraînement normal et
    nécessaire, pas une erreur — l'ambre dit « à surveiller » sans dramatiser (point de crédibilité lifting).
- **Mapping indice** : `50 + 50·(performance/fitness)`, clampé [0,100] — indépendant de l'échelle NORM (le
  ratio l'annule). 50 = neutre ; sans données (fitness≈0) → 50/Frais.
- **Portée** : **miroir uniquement** au sprint 4 (seul athlète entraîné) et seulement **après ≥1 séance** —
  sinon **absence de section** (pas de « Frais 50 » de bruit). Activé pour les virtuels au sprint 6.
- **Accessibilité** : `role="img"` + `aria-label` synthétique (« Forme : 72 sur 100, affûté »). Contrastes AA
  dans les deux modes.
- **Async** : 2ᵉ fetch (`/api/athletes/:id/condition`) ; **dégradation gracieuse** — un échec n'enlève pas le
  reste de la fiche, la section Forme est simplement omise (doctrine async-states).

---

## 5. Patterns & layouts canoniques

### 5.1 Layout App (base de l'app connectée)
`[ sidebar 240 | ( top bar 56 / content ) ]`. Content : `max-w-[1200px] mx-auto px-8 py-8`. C'est le châssis par défaut (écurie, programmes, calendrier).

### 5.2 Layout Focus / Form
Colonne centrée `max-w-[420px] mx-auto`, pleine hauteur, fond `surface-base`, pas de sidebar. Login, onboarding, paramètres. Le wordmark Cormorant en haut, généreusement espacé.

### 5.3 Layout Dashboard / Analytics
Grille de cards multi-colonnes `grid grid-cols-12 gap-6` (auto-fit `minmax(280px,1fr)` en responsive). Stat blocks en haut (rangée de 3–4), charts en dessous (`col-span-8` + `col-span-4`). Pour la page d'accueil d'un athlète, les comparaisons.

### 5.4 Layout Détail entité
`header héros` (avatar/nom en `h1` Cormorant + méta + actions) → `barre d'onglets` (`Vue d'ensemble · Programme · Historique · Génétique`) → `content` propre à l'onglet. Page d'un athlète, d'un programme.

```html
<!-- barre d'onglets -->
<div class="flex gap-1 border-b border-[var(--border-default)]">
  <button class="px-4 h-10 font-sans text-body text-[var(--text-primary)]
    border-b-2 border-[var(--accent)] -mb-px">Vue d'ensemble</button>
  <button class="px-4 h-10 font-sans text-body text-[var(--text-tertiary)]
    border-b-2 border-transparent hover:text-[var(--text-primary)]">Programme</button>
</div>
```

---

## 6. Voix éditoriale

Toujours : **français**, **deuxième personne (« tu »)**, rigueur technique sans jargon obscur, **zéro superlatif marketing**, touche d'érudition sobre quand c'est juste. Jamais « coach motivateur ».

| Contexte | ✅ Atlas | ❌ À éviter |
|---|---|---|
| **Login** | « Reprends ton écurie là où tu l'as laissée. » | « Bienvenue, champion ! 💪 » |
| **Erreur (champ)** | « Ce poids dépasse le 1RM estimé de ton athlète. Vérifie la charge. » | « Oups ! Quelque chose s'est mal passé. » |
| **Succès (séance loggée)** | « Séance enregistrée. Ton athlète miroir a absorbé la charge. » | « Bravo, tu déchires ! » |
| **Alerte deload** | « Fatigue à 68 %. Banister (1975) situe ici le seuil où la charge nuit plus qu'elle ne construit — un deload est recommandé. » | « ⚠️ ATTENTION : repos obligatoire ! » |
| **PR battu** | « 120 kg au développé couché. +5 kg sur le cycle. Nouveau record pour Marcus. » | « 🔥 RECORD EXPLOSÉ ! INCROYABLE ! » |
| **Recrutement** | « Marcus Vélaris rejoint ton écurie. Profil génétique : répondeur rapide à l'hypertrophie, récupération lente. » | « Débloque ce super athlète légendaire ! » |
| **Fin de programme** | « 12 semaines bouclées. Le 5/3/1 est désormais débloqué pour tes athlètes virtuels. » | « Niveau terminé ! +500 XP ! » |
| **Onboarding** | « Atlas s'appuie sur le modèle Fitness-Fatigue. Plus tu charges, plus tu progresses — jusqu'à ce que la fatigue dépasse la forme. » | « Prêt à devenir une légende ? » |
| **État vide (écurie)** | « Ton écurie est vide. Recrute un premier athlète pour commencer un cycle. » | « Rien à voir ici pour l'instant 🤷 » |
| **Confirmation destructive** | « Retirer Marcus mettra fin à son cycle en cours. Cette action est définitive. » | « Es-tu vraiment, vraiment sûr ?? » |

**Termes techniques** : assumés (1RM, RPE, e1RM, SAID, Wilks, mésocycle), expliqués au survol via tooltip si nécessaire — jamais vulgarisés à l'excès.

---

## 7. Iconographie

- **Lucide** exclusivement (stroke 1.5–2px, cohérent avec la sobriété).
- Tailles : **16** (inline, dans le texte/badges), **20** (boutons, nav), **24** (titres de section, états vides).
- `stroke-width` par défaut 1.75. Couleur héritée du texte (`currentColor`), jamais multicolore.
- **Règle** : icônes strictement UI/fonctionnelles. Aucune illustration décorative, aucune icône « pour faire joli », aucun emoji dans l'interface produit.
- Icônes récurrentes : `dumbbell` (séance), `trending-up/down` (delta), `activity` (fatigue/forme), `calendar` (cycle), `users` (écurie), `target` (objectif), `flame`→**non** (trop gym, préférer `zap` ou `activity`).

---

## 8. Motion

**Philosophie** : économie. Une transition clarifie un changement d'état ; elle n'impressionne jamais. Si on ne saurait pas dire ce qu'elle explique, on la retire. Respecter `prefers-reduced-motion` (couper toute animation non essentielle).

**Durées & easing**
```css
--ease-out:    cubic-bezier(0.22, 1, 0.36, 1);   /* sorties, entrées d'éléments */
--ease-in-out: cubic-bezier(0.45, 0, 0.40, 1);   /* déplacements, réversibles */
--dur-instant: 100ms;  /* hover, pressions, micro-feedback */
--dur-fast:    200ms;  /* focus ring, tab switch, badges */
--dur-base:    300ms;  /* entrée modale/popover, transition de thème */
```

**Patterns canoniques**

| Pattern | Spéc |
|---|---|
| **Hover** (bouton, ligne, card) | `transition-colors` (+ `border`), `--dur-instant`, `--ease-out`. Jamais de scale. |
| **Focus** | Anneau bronze qui apparaît en `--dur-fast`. Pas de pulsation. |
| **Modale / popover (entrée)** | `opacity 0→1` + `translateY(6px→0)` + `scale(0.98→1)`, `--dur-base`, `--ease-out`. Overlay en `opacity` simple. |
| **Tab switch** | Le souligné bronze glisse vers l'onglet actif (`transform`), `--dur-fast`, `--ease-in-out`. Le contenu fait un `opacity 0→1` court (120ms). |
| **Page transition** | Fondu `opacity` + `translateY(8px→0)` sur le content, `--dur-base`. Pas de slide horizontal, pas de parallaxe. |
| **Toggle thème** | `background-color`/`color` en `--dur-base ease-in-out` sur les surfaces. Pas de flash blanc. |

> Interdits : confettis, rebonds (`bounce`), scale au hover, animations en boucle décoratives, parallaxe, skeleton « shimmer » trop voyant (préférer un fondu sobre).

---

## 9. Référence Tailwind (extrait `tailwind.config.js`)

```js
module.exports = {
  darkMode: ['selector', '[data-theme="dark"]'],
  theme: {
    extend: {
      colors: {
        surface: {
          base: 'var(--surface-base)', sunken: 'var(--surface-sunken)',
          raised: 'var(--surface-raised)', 'raised-2': 'var(--surface-raised-2)',
        },
        ink: {
          primary: 'var(--text-primary)', secondary: 'var(--text-secondary)',
          tertiary: 'var(--text-tertiary)', disabled: 'var(--text-disabled)',
          inverted: 'var(--text-inverted)',
        },
        accent: {
          DEFAULT: 'var(--accent)', hover: 'var(--accent-hover)',
          active: 'var(--accent-active)', surface: 'var(--accent-surface)',
        },
        success: 'var(--success)', warning: 'var(--warning)',
        danger:  'var(--danger)',  info:    'var(--info)',
      },
      borderColor: {
        subtle: 'var(--border-subtle)', DEFAULT: 'var(--border-default)',
        strong: 'var(--border-strong)',
      },
      fontFamily: { display: ['Cormorant','serif'], sans: ['"IBM Plex Sans"','sans-serif'], mono: ['"IBM Plex Mono"','monospace'] },
      // fontSize : cf. §2.2
    },
  },
};
```
> Avec ces alias, `bg-surface-raised`, `text-ink-secondary`, `border-strong`, `text-accent` deviennent disponibles sans arbitrary values.

---

*Fin du document. Le minimum vital — tokens, 7 composants, 4 layouts — est posé. Le reste émergera à l'usage.*
