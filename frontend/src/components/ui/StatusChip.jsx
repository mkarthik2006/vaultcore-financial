const TONES = {
  neutral: { bg: "var(--vc-slate-100)", fg: "var(--vc-slate-700)" },
  brand: { bg: "var(--vc-slate-900)", fg: "#fff" },
  success: { bg: "var(--vc-emerald-100)", fg: "var(--vc-emerald-700)" },
  warning: { bg: "var(--vc-amber-100)", fg: "var(--vc-amber-700)" },
  danger: { bg: "var(--vc-red-100)", fg: "var(--vc-red-700)" },
  info: { bg: "var(--vc-blue-100)", fg: "var(--vc-blue-700)" },
};

/** Small rounded status/role/badge pill. Used for roles, transfer states, P&L direction, etc. */
export default function StatusChip({ tone = "neutral", icon, children }) {
  const { bg, fg } = TONES[tone] || TONES.neutral;
  return (
    <span
      className="d-inline-flex align-items-center gap-1 fw-semibold"
      style={{
        background: bg,
        color: fg,
        borderRadius: "var(--vc-radius-pill)",
        padding: "0.25rem 0.65rem",
        fontSize: "var(--vc-text-xs)",
        lineHeight: 1.4,
        whiteSpace: "nowrap",
      }}
    >
      {icon && <i className={`bi ${icon}`} aria-hidden="true" />}
      {children}
    </span>
  );
}
