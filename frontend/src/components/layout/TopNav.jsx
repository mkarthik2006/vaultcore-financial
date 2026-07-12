import { useState } from "react";
import { useAuthStore, selectUsername, selectIsAdmin } from "../../store/authStore";
import { logout } from "../../services/auth";
import StatusChip from "../ui/StatusChip";

export default function TopNav({ onToggleSidebar }) {
  const username = useAuthStore(selectUsername);
  const admin = useAuthStore(selectIsAdmin);
  const [menuOpen, setMenuOpen] = useState(false);

  return (
    <header className="vc-topnav d-flex align-items-center px-2 px-md-3 gap-2">
      <button
        type="button"
        className="btn btn-link text-white d-md-none p-2"
        aria-label="Toggle navigation menu"
        onClick={onToggleSidebar}
      >
        <i className="bi bi-list fs-4" aria-hidden="true" />
      </button>

      <a href="/dashboard" className="d-flex align-items-center gap-2 text-decoration-none text-white me-auto">
        <img src="/vaultcore-logo.svg" alt="" width={22} height={22} aria-hidden="true" />
        <span className="fw-semibold" style={{ letterSpacing: "0.02em" }}>VaultCore</span>
      </a>

      <div className="position-relative">
        <button
          type="button"
          className="btn btn-link text-white text-decoration-none d-flex align-items-center gap-2 py-1 px-2"
          onClick={() => setMenuOpen((v) => !v)}
          aria-haspopup="menu"
          aria-expanded={menuOpen}
        >
          <span
            className="d-inline-flex align-items-center justify-content-center rounded-circle fw-semibold"
            style={{ width: 30, height: 30, background: "var(--vc-blue-600)", fontSize: "0.8rem" }}
            aria-hidden="true"
          >
            {(username || "?").slice(0, 1).toUpperCase()}
          </span>
          <span className="small d-none d-sm-inline">{username || "Account"}</span>
          <i className="bi bi-chevron-down small d-none d-sm-inline" aria-hidden="true" />
        </button>

        {menuOpen && (
          <>
            <div
              className="position-fixed top-0 start-0 w-100 h-100"
              style={{ zIndex: 1049 }}
              onClick={() => setMenuOpen(false)}
              aria-hidden="true"
            />
            <div
              role="menu"
              className="vc-card position-absolute end-0 mt-2 p-2"
              style={{ minWidth: 220, zIndex: 1050 }}
            >
              <div className="px-2 py-2 border-bottom mb-1">
                <div className="fw-semibold small text-truncate" style={{ color: "var(--vc-text)" }}>{username}</div>
                <StatusChip tone={admin ? "brand" : "neutral"} icon={admin ? "bi-shield-lock-fill" : "bi-person-fill"}>
                  {admin ? "Administrator" : "User"}
                </StatusChip>
              </div>
              <button
                type="button"
                role="menuitem"
                className="btn btn-sm btn-light w-100 text-start d-flex align-items-center gap-2"
                onClick={logout}
              >
                <i className="bi bi-box-arrow-right" aria-hidden="true" />
                Log out
              </button>
            </div>
          </>
        )}
      </div>
    </header>
  );
}
