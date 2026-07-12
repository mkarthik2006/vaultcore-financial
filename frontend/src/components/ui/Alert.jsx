const TONES = {
  danger: { icon: "bi-exclamation-triangle-fill", bg: "var(--vc-red-50)", border: "var(--vc-red-500)", fg: "var(--vc-red-700)" },
  success: { icon: "bi-check-circle-fill", bg: "var(--vc-emerald-50)", border: "var(--vc-emerald-500)", fg: "var(--vc-emerald-700)" },
  warning: { icon: "bi-exclamation-circle-fill", bg: "var(--vc-amber-50)", border: "var(--vc-amber-500)", fg: "var(--vc-amber-700)" },
  info: { icon: "bi-info-circle-fill", bg: "var(--vc-blue-50)", border: "var(--vc-blue-500)", fg: "var(--vc-blue-700)" },
};

/** Inline, accessible status banner. aria-live is left to the caller since urgency varies by context. */
export default function Alert({ tone = "info", title, children, className = "", ...rest }) {
  const t = TONES[tone] || TONES.info;
  return (
    <div
      className={`d-flex gap-2 align-items-start rounded-3 px-3 py-2 ${className}`}
      style={{ background: t.bg, borderLeft: `4px solid ${t.border}`, color: t.fg }}
      role={tone === "danger" ? "alert" : "status"}
      {...rest}
    >
      <i className={`bi ${t.icon} mt-1`} aria-hidden="true" />
      <div>
        {title && <div className="fw-semibold">{title}</div>}
        <div className="small">{children}</div>
      </div>
    </div>
  );
}
