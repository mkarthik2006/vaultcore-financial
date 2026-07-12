/** steps: string[]; activeIndex: 0-based index of the current step. Used by the Send Money wizard. */
export default function StepIndicator({ steps, activeIndex }) {
  return (
    <nav aria-label="Progress" className="mb-4">
      <ol className="d-flex list-unstyled gap-2 gap-sm-3 flex-wrap justify-content-center m-0">
        {steps.map((label, i) => {
          const state = i < activeIndex ? "done" : i === activeIndex ? "current" : "upcoming";
          return (
            <li key={label} className="d-flex align-items-center gap-2">
              <span
                className="d-inline-flex align-items-center justify-content-center rounded-circle fw-semibold flex-shrink-0"
                style={{
                  width: 28,
                  height: 28,
                  fontSize: "0.8rem",
                  background: state === "upcoming" ? "var(--vc-slate-100)" : "var(--vc-slate-900)",
                  color: state === "upcoming" ? "var(--vc-slate-500)" : "#fff",
                }}
                aria-current={state === "current" ? "step" : undefined}
              >
                {state === "done" ? <i className="bi bi-check2" aria-hidden="true" /> : i + 1}
              </span>
              <span
                className="small"
                style={{
                  fontWeight: state === "current" ? 600 : 500,
                  color: state === "upcoming" ? "var(--vc-text-muted)" : "var(--vc-text)",
                }}
              >
                {label}
              </span>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
