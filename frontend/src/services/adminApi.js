import { apiFetch } from "./apiClient";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8082";

async function handleJson(res) {
  if (!res.ok) {
    let message = "Request failed";
    try {
      const payload = await res.json();
      message = payload.message || payload.error || message;
    } catch (e) {
      // ignore
    }
    throw new Error(message);
  }
  return res.json();
}

export async function createUser(payload) {
  const res = await apiFetch(`${API_BASE_URL}/api/v1/admin/users`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return handleJson(res);
}

export async function createAccount(payload) {
  const res = await apiFetch(`${API_BASE_URL}/api/v1/admin/accounts`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return handleJson(res);
}