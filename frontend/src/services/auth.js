import Keycloak from "keycloak-js";

const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:8082/auth",
  realm: import.meta.env.VITE_KEYCLOAK_REALM || "vaultcore",
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "vaultcore-frontend",
});

let accessToken = null;

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
    return true;
  } catch {
    accessToken = null;
    return false;
  }
}

export function getKeycloak() {
  return keycloak;
}