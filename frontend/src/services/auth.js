let accessToken = null;

export function setAccessToken(token) {
  accessToken = token;
}

export function getAccessToken() {
  return accessToken;
}

export async function login(username, password) {
  const res = await fetch("/auth/login", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    credentials: "include",
    body: JSON.stringify({ username, password })
  });

  if (!res.ok) throw new Error("Invalid credentials");
  const data = await res.json();
  setAccessToken(data.accessToken);
  return data;
}

export async function refreshToken() {
  const res = await fetch("/auth/refresh", {
    method: "POST",
    credentials: "include"
  });
  if (!res.ok) throw new Error("Refresh failed");
  const data = await res.json();
  setAccessToken(data.accessToken);
  return data;
}

export async function bootstrapAuth() {
  try {
    await refreshToken();
    return true;
  } catch {
    return false;
  }
}