import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:8081",
  realm: import.meta.env.VITE_KEYCLOAK_REALM || "vaultcore",
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "vaultcore-frontend",
});

let accessToken = null;

/**
 * Compliance:
 * - Token is sourced from Keycloak session in-memory (no localStorage/sessionStorage).
 * - Caller should only use when authenticated.
 */
export function getAccessToken() {
  if (!keycloak.authenticated || !accessToken) {
    throw new Error("Not authenticated (no access token available).");
  }
  return accessToken;
}

export async function initAuth() {
  const authenticated = await keycloak.init({
    pkceMethod: "S256",
    onLoad: "check-sso",
    silentCheckSsoRedirectUri: window.location.origin + "/silent-check-sso.html",
  });

  accessToken = keycloak.token || null;
  return authenticated;
}

export async function login() {
  await keycloak.login();
}

export async function logout() {
  await keycloak.logout({ redirectUri: window.location.origin });
}

export function isAuthenticated() {
  return !!keycloak.authenticated;
}

export async function refreshToken() {
  if (!keycloak.token) return;
  const refreshed = await keycloak.updateToken(30);
  if (refreshed) {
    accessToken = keycloak.token;
  } else {
    // even if not refreshed, token may still be valid; keep it in sync anyway
    accessToken = keycloak.token;
  }
}

export function getKeycloak() {
  return keycloak;
}