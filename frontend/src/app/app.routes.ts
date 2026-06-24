import { Routes } from '@angular/router';
import { authGuard } from './auth/auth.guard';
import { LoginPage } from './auth/pages/login-page';
import { LinkSentPage } from './auth/pages/link-sent-page';
import { AuthCallbackPage } from './auth/pages/auth-callback-page';
import { OnboardingPage } from './auth/pages/onboarding-page';
import { HomePage } from './auth/pages/home-page';
import { RosterPage } from './roster/pages/roster-page';
import { MirrorCreatePage } from './roster/pages/mirror-create-page';
import { ScoutPage } from './roster/pages/scout-page';
import { AthleteDetailPage } from './roster/pages/athlete-detail-page';
import { TrainingPage } from './training/pages/training-page';
import { LogWorkoutPage } from './training/pages/log-workout-page';
import { SessionDetailPage } from './training/pages/session-detail-page';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: LoginPage },
  { path: 'login/sent', component: LinkSentPage },
  { path: 'auth/callback', component: AuthCallbackPage },
  { path: 'onboarding', component: OnboardingPage },
  { path: 'home', component: HomePage, canActivate: [authGuard] },
  { path: 'roster', component: RosterPage, canActivate: [authGuard] },
  { path: 'roster/mirror/new', component: MirrorCreatePage, canActivate: [authGuard] },
  { path: 'roster/scout', component: ScoutPage, canActivate: [authGuard] },
  { path: 'roster/athletes/:id', component: AthleteDetailPage, canActivate: [authGuard] },
  { path: 'training', component: TrainingPage, canActivate: [authGuard] },
  { path: 'training/log', component: LogWorkoutPage, canActivate: [authGuard] },
  { path: 'training/sessions/:id', component: SessionDetailPage, canActivate: [authGuard] },
  { path: '**', redirectTo: 'login' },
];
