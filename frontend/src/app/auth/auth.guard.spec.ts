import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { GuardResult, provideRouter, UrlTree } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Observable, firstValueFrom } from 'rxjs';
import { authGuard } from './auth.guard';
import { AuthService } from './auth.service';
import { CurrentUser } from './auth.models';

const USER: CurrentUser = {
  id: 'u1',
  email: 'ryan@example.com',
  displayName: 'Ryan',
  locale: 'fr',
  timezone: 'Europe/Brussels',
  createdAt: '2026-06-23T10:00:00Z',
  lastLoginAt: null,
};

function runGuard() {
  return TestBed.runInInjectionContext(() => authGuard({} as never, {} as never));
}

describe('authGuard', () => {
  let http: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
  });

  afterEach(() => http.verify());

  it('allows immediately when already authenticated (no extra request)', () => {
    auth.loadCurrentUser().subscribe();
    http.expectOne('/api/auth/me').flush(USER);

    expect(runGuard()).toBe(true);
  });

  it('rehydrates via /me and allows when a session exists', async () => {
    const promise = firstValueFrom(runGuard() as Observable<GuardResult>);
    http.expectOne('/api/auth/me').flush(USER);

    expect(await promise).toBe(true);
  });

  it('redirects to /login when /me returns 401', async () => {
    const promise = firstValueFrom(runGuard() as Observable<GuardResult>);
    http.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });

    const result = await promise;
    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/login');
  });
});
