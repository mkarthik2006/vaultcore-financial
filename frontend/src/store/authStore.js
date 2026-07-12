import { create } from "zustand";

/**
 * Central client-side auth/session state (Zustand).
 *
 * Keycloak remains the source of truth for tokens; this store holds the derived, render-facing
 * state (authenticated flag, realm roles, username) so components subscribe reactively instead of
 * each calling into the Keycloak adapter. Populated by `services/auth.js`.
 */
export const useAuthStore = create((set) => ({
  authenticated: false,
  roles: [],
  username: null,
  sessionStartedAt: null,

  setAuth: ({ authenticated, roles, username, sessionStartedAt }) =>
    set({
      authenticated: !!authenticated,
      roles: roles ?? [],
      username: username ?? null,
      sessionStartedAt: sessionStartedAt ?? null,
    }),

  clearAuth: () => set({ authenticated: false, roles: [], username: null, sessionStartedAt: null }),
}));

// Selectors — keep role logic in one place.
export const selectIsAuthenticated = (s) => s.authenticated;
export const selectRoles = (s) => s.roles;
export const selectIsAdmin = (s) => s.roles.includes("ADMIN");
export const selectUsername = (s) => s.username;
export const selectSessionStartedAt = (s) => s.sessionStartedAt;
