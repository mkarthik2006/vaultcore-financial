const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8082";

/** GET /actuator/health — permitAll, unauthenticated. Used for the dashboard's system status pill. */
export async function getSystemHealth() {
  const res = await fetch(`${API_BASE_URL}/actuator/health`, { credentials: "include" });
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const data = await res.json();
  return data.status; // "UP" | "DOWN" | ...
}
