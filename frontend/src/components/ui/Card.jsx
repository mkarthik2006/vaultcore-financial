/** Base surface for grouped content — the atomic building block of every dashboard/page card. */
export default function Card({ className = "", children, padded = true, ...rest }) {
  const padding = padded ? "p-4 p-md-4" : "";
  return (
    <div className={`vc-card ${padding} ${className}`} {...rest}>
      {children}
    </div>
  );
}

export function CardHeader({ title, subtitle, action, icon }) {
  return (
    <div className="d-flex align-items-start justify-content-between mb-3 gap-2">
      <div className="d-flex align-items-start gap-2">
        {icon && (
          <span
            className="d-inline-flex align-items-center justify-content-center rounded-3 flex-shrink-0"
            style={{ width: 36, height: 36, background: "var(--vc-slate-100)", color: "var(--vc-brand)" }}
            aria-hidden="true"
          >
            <i className={`bi ${icon}`} />
          </span>
        )}
        <div>
          <h2 className="h6 mb-0 fw-semibold" style={{ color: "var(--vc-text)" }}>{title}</h2>
          {subtitle && <p className="mb-0 small vc-text-muted">{subtitle}</p>}
        </div>
      </div>
      {action}
    </div>
  );
}
