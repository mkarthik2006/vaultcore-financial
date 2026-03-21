import { getAccessToken, refreshToken } from "./auth";

let redirecting = false;

function redirectToUnauthorized() {
  if (redirecting) return;
  redirecting = true;
  const current = window.location.pathname;
  if (current !== "/login" && current !== "/unauthorized") {
    window.location.assign("/unauthorized");
  }
}

export async function apiFetch(url, options = {}) {
  const token = getAccessToken();
  const headers = {
    ...(options.headers || {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };

  let res = await fetch(url, { ...options, headers, credentials: "include" });

  if (res.status === 401) {
    const refreshed = await refreshToken().catch(() => false);
    if (!refreshed) {
      redirectToUnauthorized();
      return res;
    }

    const retryToken = getAccessToken();
    res = await fetch(url, {
      ...options,
      headers: {
        ...(options.headers || {}),
        ...(retryToken ? { Authorization: `Bearer ${retryToken}` } : {}),
      },
      credentials: "include",
    });

    if (res.status === 401) {
      redirectToUnauthorized();
    }
  }

  return res;
}