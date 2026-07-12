import { useEffect, useState } from "react";
import { apiFetch } from "../services/apiClient";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8082";

export default function AuditPage() {
  const [rows, setRows] = useState([]);
  const [status, setStatus] = useState("loading");

  useEffect(() => {
    let mounted = true;

    async function load() {
      try {
        const res = await apiFetch(`${API_BASE_URL}/api/v1/audit`);
        if (!res.ok) throw new Error("unavailable");
        const data = await res.json();
        if (mounted) {
          setRows(Array.isArray(data) ? data : []);
          setStatus("live");
        }
      } catch {
        if (mounted) {
          setRows([]);
          setStatus("mock");
        }
      }
    }

    load();
    return () => {
      mounted = false;
    };
  }, []);

  return (
    <main style={{ maxWidth: 920, margin: "2rem auto", padding: "0 1rem" }}>
      <h2>Audit Viewer</h2>
      {status === "loading" && <p>Loading audit data...</p>}
      {status === "mock" && <p>Audit data not available (mock mode).</p>}
      {status === "live" && (
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr>
              <th align="left">User</th>
              <th align="left">Action</th>
              <th align="left">Timestamp</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r, i) => (
              <tr key={i}>
                <td>{r.user ?? "-"}</td>
                <td>{r.action ?? "-"}</td>
                <td>{r.timestamp ?? "-"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  );
}