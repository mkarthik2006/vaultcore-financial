import { apiFetch } from "./apiClient";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8082";

async function handleJson(res) {
  if (!res.ok) {
    let message = "Request failed";
    try {
      const payload = await res.json();
      message = payload.message || payload.error || message;
    } catch {
      // response body was not JSON; keep the default message
    }
    throw new Error(message);
  }
  return res.json();
}

export async function getPortfolio() {
  const res = await apiFetch(`${API_BASE_URL}/api/v1/portfolio`);
  return handleJson(res);
}

export async function addHolding(payload) {
  const res = await apiFetch(`${API_BASE_URL}/api/v1/portfolio/holdings`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  return handleJson(res);
}

export async function getValuation() {
  const res = await apiFetch(`${API_BASE_URL}/api/v1/portfolio/valuation`);
  return handleJson(res);
}