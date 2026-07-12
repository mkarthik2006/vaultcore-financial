import Keycloak from "keycloak-js";
import { useAuthStore } from "../store/authStore";

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:8082/auth",
  realm: import.meta.env.VITE_KEYCLOAK_REALM || "vaultcore",
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "vaultcore-frontend",
});

let accessToken = null;

/** Pushes the current Keycloak session into the Zustand auth store. */
function syncAuthStore() {
  // auth_time is the moment the user actually authenticated with Keycloak (stable across silent
  // token refreshes); iat is only a fallback for tokens that omit it. Neither is a true
  // "last login" record (the backend exposes no such endpoint) — the UI must label this honestly.
  const claims = keycloak.tokenParsed;
  const epochSeconds = claims?.auth_time ?? claims?.iat ?? null;
  useAuthStore.getState().setAuth({
    authenticated: !!keycloak.authenticated,
    roles: claims?.realm_access?.roles ?? [],
    username: claims?.preferred_username ?? null,
    sessionStartedAt: epochSeconds ? epochSeconds * 1000 : null,
  });
}

export function getAccessToken() {
  return accessToken;
}

export async function initAuth() {
  const authenticated = await keycloak.init({
    pkceMethod: "S256",
    onLoad: "check-sso",
    silentCheckSsoRedirectUri: window.location.origin + "/silent-check-sso.html",
  });

  accessToken = keycloak.token || null;
  syncAuthStore();
  return authenticated;
}

export async function login() {
  await keycloak.login();
}

export async function logout() {
  useAuthStore.getState().clearAuth();
  await keycloak.logout({ redirectUri: window.location.origin });
}

export function isAuthenticated() {
  return !!keycloak.authenticated;
}

export function getRoles() {
  return keycloak.tokenParsed?.realm_access?.roles ?? [];
}

export function hasRole(role) {
  return getRoles().includes(role);
}

export function isAdmin() {
  return hasRole("ADMIN");
}

export async function refreshToken() {
  if (!keycloak.authenticated) return false;
  try {
    await keycloak.updateToken(30);
    accessToken = keycloak.token || null;
    syncAuthStore();
    return true;
  } catch {
    accessToken = null;
    useAuthStore.getState().clearAuth();
    return false;
  }
}

export function getKeycloak() {
  return keycloak;
}
