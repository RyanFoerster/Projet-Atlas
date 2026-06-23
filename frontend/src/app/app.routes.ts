import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { LoginPage } from './auth/pages/login-page';
import { LinkSentPage } from './auth/pages/link-sent-page';
import { AuthCallbackPage } from './auth/pages/auth-callback-page';
import { OnboardingPage } from './auth/pages/onboarding-page';
import { HomePage } from './auth/pages/home-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: LoginPage },
  { path: 'login/sent', component: LinkSentPage },
  { path: 'auth/callback', component: AuthCallbackPage },
  { path: 'onboarding', component: OnboardingPage },
  { path: 'home', component: HomePage, canActivate: [authGuard] },
  { path: '**', redirectTo: 'login' },
];
