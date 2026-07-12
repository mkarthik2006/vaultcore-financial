import { Link } from "react-router-dom";
import Button from "../../components/ui/Button";

/** Shared full-page layout for 403/404/500 — keeps the three error pages visually identical. */
export default function StatusPage({ code, icon, title, description, primaryAction, children }) {
  return (
    <div
      className="d-flex align-items-center justify-content-center min-vh-100 px-3"
      style={{ background: "var(--vc-slate-900)" }}
    >
      <div className="vc-card p-4 p-md-5 text-center vc-fade-in" style={{ maxWidth: 460, width: "100%" }}>
        <div
          className="d-inline-flex align-items-center justify-content-center rounded-circle mb-3"
          style={{ width: 64, height: 64, background: "var(--vc-slate-100)", color: "var(--vc-slate-700)" }}
        >
          <i className={`bi ${icon} fs-3`} aria-hidden="true" />
        </div>
        <div className="small fw-semibold vc-text-muted mb-1" style={{ letterSpacing: "0.08em" }}>ERROR {code}</div>
        <h1 className="h4 mb-2">{title}</h1>
        <p className="small vc-text-muted mb-4">{description}</p>
        {children}
        {primaryAction || (
          <Link to="/dashboard" className="btn vc-btn-brand">
            <i className="bi bi-house-door-fill me-2" aria-hidden="true" />
            Back to dashboard
          </Link>
        )}
      </div>
    </div>
  );
}
