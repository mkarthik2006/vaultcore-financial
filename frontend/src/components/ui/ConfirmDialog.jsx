import { useEffect, useRef } from "react";

/**
 * Accessible confirmation modal (no Bootstrap JS bundle dependency — plain React so focus trapping
 * and Escape-to-close are explicit and testable). Renders nothing when closed.
 */
export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  tone = "brand",
  busy = false,
  onConfirm,
  onCancel,
}) {
  const confirmRef = useRef(null);

  useEffect(() => {
    if (!open) return;
    confirmRef.current?.focus();
    function onKeyDown(e) {
      if (e.key === "Escape") onCancel?.();
    }
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [open, onCancel]);

  if (!open) return null;

  const confirmClass = tone === "danger" ? "btn-danger" : "vc-btn-brand btn";

  return (
    <div
      className="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center"
      style={{ background: "rgba(15, 23, 42, 0.5)", zIndex: 1090 }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="vc-confirm-title"
      onMouseDown={(e) => { if (e.target === e.currentTarget) onCancel?.(); }}
    >
      <div className="vc-card p-4" style={{ maxWidth: 420, width: "90%" }}>
        <h2 id="vc-confirm-title" className="h5 mb-2">{title}</h2>
        <p className="small vc-text-muted mb-4">{message}</p>
        <div className="d-flex justify-content-end gap-2">
          <button type="button" className="btn btn-outline-secondary" onClick={onCancel} disabled={busy}>
            {cancelLabel}
          </button>
          <button
            type="button"
            ref={confirmRef}
            className={confirmClass}
            onClick={onConfirm}
            disabled={busy}
          >
            {busy ? "Working…" : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
