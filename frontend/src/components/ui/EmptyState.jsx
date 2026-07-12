/** Placeholder shown when a list/section has no data yet, with an optional call-to-action. */
export default function EmptyState({ icon = "bi-inbox", title, description, action }) {
  return (
    <div className="text-center py-5 px-3">
      <div
        className="d-inline-flex align-items-center justify-content-center rounded-circle mb-3"
        style={{ width: 56, height: 56, background: "var(--vc-slate-100)", color: "var(--vc-slate-400)" }}
      >
        <i className={`bi ${icon} fs-4`} aria-hidden="true" />
      </div>
      <p className="fw-semibold mb-1" style={{ color: "var(--vc-text)" }}>{title}</p>
      {description && <p className="small vc-text-muted mb-3" style={{ maxWidth: 360, marginInline: "auto" }}>{description}</p>}
      {action}
    </div>
  );
}
