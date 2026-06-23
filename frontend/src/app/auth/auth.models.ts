/** Le Player courant, tel que renvoyé par GET /api/auth/me (miroir de CurrentUserDto côté backend). */
export interface CurrentUser {
  id: string;
  email: string;
  displayName: string;
  locale: string;
  timezone: string;
  createdAt: string;
  lastLoginAt: string | null;
}

/** Réponse de la consommation d'un lien magique. `newUser` route vers /onboarding ou /home. */
export interface ConsumeResult {
  newUser: boolean;
}
