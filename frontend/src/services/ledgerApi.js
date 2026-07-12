import { apiFetch } from "./apiClient";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8082";

/** GET /api/v1/ledger/balance — the only ledger endpoint the backend exposes (no history/list). */
export async function getBalance(accountNumber, currency) {
  const params = new URLSearchParams({ accountNumber, currency });
  const res = await apiFetch(`${API_BASE_URL}/api/v1/ledger/balance?${params.toString()}`);
  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    try {
      const data = await res.json();
      message = data?.message || data?.error || message;
    } catch {
      // response body wasn't JSON (e.g., plain 404 text); keep the default message
    }
    throw new Error(message);
  }
  // The controller returns a bare BigDecimal, which Jackson serializes as a raw JSON number.
  return res.json();
}
