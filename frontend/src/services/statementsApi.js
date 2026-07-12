import { apiFetch } from "./apiClient";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8082";

function filenameFromDisposition(header, fallback) {
  const match = /filename="?([^";]+)"?/i.exec(header || "");
  return match ? match[1] : fallback;
}

/**
 * GET /api/v1/statements/monthly?accountNumber=&month=YYYY-MM — returns raw PDF bytes.
 * Fetches the blob, then triggers a browser download via a throwaway object URL.
 */
export async function downloadMonthlyStatement(accountNumber, month) {
  const params = new URLSearchParams({ accountNumber, month });
  const res = await apiFetch(`${API_BASE_URL}/api/v1/statements/monthly?${params.toString()}`);

  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    try {
      const data = await res.json();
      message = data?.message || data?.error || message;
    } catch {
      // non-JSON error body (e.g., a 404 with no matching statement); keep the default message
    }
    throw new Error(message);
  }

  const blob = await res.blob();
  const filename = filenameFromDisposition(
    res.headers.get("Content-Disposition"),
    `statement-${accountNumber}-${month}.pdf`
  );

  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);

  return { filename };
}
