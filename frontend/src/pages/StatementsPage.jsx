import { useState } from "react";
import { downloadMonthlyStatement } from "../services/statementApi";

export default function StatementsPage() {
  const [accountNumber, setAccountNumber] = useState("");
  const [month, setMonth] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function onDownload(e) {
    e.preventDefault();
    setLoading(true);
    setError("");
    try {
      const blob = await downloadMonthlyStatement(accountNumber, month);
      const href = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = href;
      a.download = `statement-${accountNumber}-${month}.pdf`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(href);
    } catch (err) {
      setError(err?.message || "Failed to download statement");
    } finally {
      setLoading(false);
    }
  }

  return (
    <main style={{ maxWidth: 720, margin: "2rem auto", padding: "0 1rem" }}>
      <h2>Monthly Statements</h2>
      {error && <p style={{ color: "crimson" }}>{error}</p>}

      <form onSubmit={onDownload} style={{ display: "grid", gap: 10, maxWidth: 380 }}>
        <input
          placeholder="Account Number (e.g., A001)"
          value={accountNumber}
          onChange={(e) => setAccountNumber(e.target.value)}
          required
        />
        <input type="month" value={month} onChange={(e) => setMonth(e.target.value)} required />
        <button type="submit" disabled={loading}>
          {loading ? "Downloading..." : "Download PDF"}
        </button>
      </form>
    </main>
  );
}