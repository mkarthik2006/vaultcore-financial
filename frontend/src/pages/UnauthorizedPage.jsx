import { Link } from "react-router-dom";

export default function UnauthorizedPage() {
  return (
    <main style={{ maxWidth: 700, margin: "4rem auto", padding: "1rem" }}>
      <div style={{ background: "#fff", border: "1px solid #fecaca", borderRadius: 12, padding: "1.5rem" }}>
        <h2 style={{ color: "#991b1b", marginTop: 0 }}>Unauthorized</h2>
        <p>Your session is invalid/expired or you do not have permission.</p>
        <div style={{ display: "flex", gap: 12 }}>
          <Link to="/login">Go to Login</Link>
          <Link to="/dashboard">Go to Dashboard</Link>
        </div>
      </div>
    </main>
  );
}