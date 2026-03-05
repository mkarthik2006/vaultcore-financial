import { apiFetch } from "./apiClient";

const API_BASE = import.meta.env.VITE_API_BASE ?? "";

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

export async function getPortfolio() {
  const res = await apiFetch(`${API_BASE}/api/v1/portfolio`);
  return handleJson(res);
}

export async function addHolding(payload) {
  const res = await apiFetch(`${API_BASE}/api/v1/portfolio/holdings`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  return handleJson(res);
}

export async function getValuation() {
  const res = await apiFetch(`${API_BASE}/api/v1/portfolio/valuation`);
  return handleJson(res);
}