import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection()],
    });
  });

  it('defaults to dark when no preference is stored', () => {
    // jsdom n'implémente pas matchMedia → on retombe sur le défaut signature.
    const service = TestBed.inject(ThemeService);
    expect(service.theme()).toBe('dark');
    expect(service.isDark()).toBe(true);
  });

  it('reads an explicit stored preference on init', () => {
    localStorage.setItem('atlas-theme', 'light');
    const service = TestBed.inject(ThemeService);
    expect(service.theme()).toBe('light');
  });

  it('toggles and persists the choice', () => {
    const service = TestBed.inject(ThemeService);
    service.toggle();
    expect(service.theme()).toBe('light');
    expect(localStorage.getItem('atlas-theme')).toBe('light');
    service.toggle();
    expect(service.theme()).toBe('dark');
    expect(localStorage.getItem('atlas-theme')).toBe('dark');
  });

  it('applies data-theme to <html> reactively', () => {
    const service = TestBed.inject(ThemeService);
    TestBed.tick(); // flush des effects en zoneless
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');

    service.setTheme('light');
    TestBed.tick();
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
  });
});
