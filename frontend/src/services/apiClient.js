import { getAccessToken, refreshToken } from "./auth";

export async function apiFetch(url, options = {}) {
  const token = getAccessToken();
  const headers = {
    ...(options.headers || {}),
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };

  let res = await fetch(url, { ...options, headers, credentials: "include" });

  if (res.status === 401) {
    await refreshToken();
    const retryToken = getAccessToken();
    res = await fetch(url, {
      ...options,
      headers: {
        ...(options.headers || {}),
        Authorization: `Bearer ${retryToken}`,
      },
      credentials: "include",
    });
  }

  return res;
}