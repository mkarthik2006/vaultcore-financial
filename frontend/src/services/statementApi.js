import { apiFetch } from "./apiClient";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8082";

export async function downloadMonthlyStatement(accountNumber, month) {
  const params = new URLSearchParams({
    accountNumber: accountNumber.trim(),
    month: month.trim(),
  });

  const res = await apiFetch(`${API_BASE_URL}/api/v1/statements/monthly?${params.toString()}`, {
    method: "GET",
  });

  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    try {
      const data = await res.json();
      message = data?.message || data?.error || message;
    } catch {
      // ignore
    }
    throw new Error(message);
  }

  return res.blob();
}