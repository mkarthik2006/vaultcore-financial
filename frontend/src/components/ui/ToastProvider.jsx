import { useCallback, useRef, useState } from "react";
import { ToastContext } from "./toastContext";

const TONE_ICON = {
  success: "bi-check-circle-fill text-success",
  danger: "bi-x-circle-fill text-danger",
  info: "bi-info-circle-fill text-primary",
  warning: "bi-exclamation-circle-fill text-warning",
};

/**
 * Lightweight toast system (no external dependency). Renders Bootstrap-styled toasts in a fixed
 * viewport region; call useToast().push(...) from anywhere inside the tree to fire a transient
 * confirmation without stealing focus from the current form (unlike inline alerts, which stay put
 * for as long as the error/condition holds).
 */
export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);
  const counter = useRef(0);

  const dismiss = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const push = useCallback((message, { tone = "info", duration = 4000 } = {}) => {
    const id = ++counter.current;
    setToasts((prev) => [...prev, { id, message, tone }]);
    if (duration > 0) {
      setTimeout(() => dismiss(id), duration);
    }
    return id;
  }, [dismiss]);

  return (
    <ToastContext.Provider value={{ push, dismiss }}>
      {children}
      <div
        className="toast-container position-fixed bottom-0 end-0 p-3"
        style={{ zIndex: 1080 }}
        aria-live="polite"
        aria-atomic="true"
      >
        {toasts.map((t) => (
          <div key={t.id} className="toast show vc-card border-0 mb-2" role="status">
            <div className="d-flex align-items-center gap-2 p-3">
              <i className={`bi ${TONE_ICON[t.tone] || TONE_ICON.info}`} aria-hidden="true" />
              <div className="small flex-grow-1">{t.message}</div>
              <button
                type="button"
                className="btn-close"
                aria-label="Dismiss notification"
                onClick={() => dismiss(t.id)}
              />
            </div>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
}
