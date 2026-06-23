import { Injectable, computed, effect, signal } from '@angular/core';

export type Theme = 'light' | 'dark';

const STORAGE_KEY = 'atlas-theme';

/**
 * Gère le thème (dark/light) de l'application, en signals.
 *
 * Stratégie (ADR-016, design system §1) :
 * - dark = identité signature = défaut ;
 * - au démarrage : `localStorage` si une préférence explicite existe, sinon
 *   `prefers-color-scheme`, sinon dark ;
 * - la préférence n'est persistée qu'au choix explicite de l'utilisateur (toggle).
 *   Tant qu'il n'a pas choisi, on continue de suivre la préférence système.
 *
 * Un effect applique `data-theme` sur <html> de façon réactive. Le même attribut
 * est posé encore plus tôt par le script anti-flash de index.html (mêmes règles),
 * donc aucun flash de thème au chargement.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly _theme = signal<Theme>(this.resolveInitialTheme());

  /** Thème courant, en lecture seule. */
  readonly theme = this._theme.asReadonly();
  readonly isDark = computed(() => this._theme() === 'dark');

  constructor() {
    effect(() => {
      document.documentElement.setAttribute('data-theme', this._theme());
    });
  }

  /** Bascule dark <-> light et persiste le choix. */
  toggle(): void {
    this.setTheme(this._theme() === 'dark' ? 'light' : 'dark');
  }

  /** Fixe explicitement le thème et le persiste (choix utilisateur). */
  setTheme(theme: Theme): void {
    this._theme.set(theme);
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch {
      /* stockage indisponible (mode privé strict) : on reste en mémoire. */
    }
  }

  private resolveInitialTheme(): Theme {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored === 'light' || stored === 'dark') {
        return stored;
      }
    } catch {
      /* ignore */
    }
    try {
      if (window.matchMedia('(prefers-color-scheme: light)').matches) {
        return 'light';
      }
    } catch {
      /* matchMedia indisponible (ex. jsdom en test) : on retombe sur le défaut. */
    }
    return 'dark';
  }
}
