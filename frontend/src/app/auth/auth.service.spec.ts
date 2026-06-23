import { TestBed } from '@angular/core/testing';
import { provideZonelessChangeDetection } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { CurrentUser } from './auth.models';

const USER: CurrentUser = {
  id: 'u1',
  email: 'ryan@example.com',
  displayName: 'Ryan',
  locale: 'fr',
  timezone: 'Europe/Brussels',
  createdAt: '2026-06-23T10:00:00Z',
  lastLoginAt: '2026-06-23T10:00:00Z',
};

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideZonelessChangeDetection(), provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('posts the email when requesting a magic link', () => {
    service.requestMagicLink('ryan@example.com').subscribe();
    const req = http.expectOne('/api/auth/magic-link/request');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'ryan@example.com' });
    req.flush(null);
  });

  it('consumes a token via GET with the token param', () => {
    service.consume('tok-123').subscribe((r) => expect(r.newUser).toBe(true));
    const req = http.expectOne((r) => r.url === '/api/auth/magic-link/consume');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('token')).toBe('tok-123');
    req.flush({ newUser: true });
  });

  it('sets currentUser after completing signup', () => {
    expect(service.isAuthenticated()).toBe(false);
    service.completeSignup('Ryan').subscribe();
    http.expectOne('/api/auth/complete-signup').flush(USER);
    expect(service.currentUser()).toEqual(USER);
    expect(service.isAuthenticated()).toBe(true);
  });

  it('loads the current user via /me', () => {
    service.loadCurrentUser().subscribe();
    http.expectOne('/api/auth/me').flush(USER);
    expect(service.currentUser()).toEqual(USER);
  });

  it('clears currentUser on logout', () => {
    service.loadCurrentUser().subscribe();
    http.expectOne('/api/auth/me').flush(USER);
    expect(service.isAuthenticated()).toBe(true);

    service.logout().subscribe();
    http.expectOne('/api/auth/logout').flush(null);
    expect(service.currentUser()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
  });
});
