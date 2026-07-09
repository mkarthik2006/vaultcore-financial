import { Link } from "react-router-dom";
import { isAdmin } from "../services/auth";


export default function Dashboard() {
  return (
    <main style={{
      maxWidth: 720,
      margin: "4rem auto",
      background: "#fff",
      padding: "2rem",
      borderRadius: "12px",
      boxShadow: "0 8px 32px rgba(16, 29, 51, 0.15)"
    }}>
      <h2 style={{ color: "#0f172a" }}>VaultCore Dashboard</h2>
      <p aria-live="polite" style={{
        color: "#16a34a",
        fontWeight: 500,
        fontSize: "1.15rem",
        marginBottom: 8
      }}>✅ Login successful.</p>
       <p>
        <Link to="/portfolio">View Portfolio</Link>
      </p>
      <p>
        <Link to="/send-money">Send Money</Link>
      </p>
      {isAdmin() && (
        <p>
          <Link to="/admin">Admin Provisioning</Link>
        </p>
      )}
      <hr style={{ margin: "2rem 0", borderColor: "#e5e7eb" }} />
      <section>
        <p style={{ fontSize: "1rem", color: "#334155" }}>
          Welcome! Use the main menu to view accounts, send money, or manage transactions.<br />
          <span style={{ fontSize: "0.9rem", color: "#64748b" }}>
            Remember to log out &mdash; your session is monitored for compliance.
          </span>
        </p>
      </section>
      <footer style={{ marginTop: "2rem", fontSize: "0.87rem", color: "#475569" }}>
        VaultCore &copy; 2026. For authorized use only.
      </footer>
    </main>
  );
}