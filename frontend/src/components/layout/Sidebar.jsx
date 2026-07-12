import { NavLink } from "react-router-dom";
import { useAuthStore, selectIsAdmin } from "../../store/authStore";
import { NAV_ITEMS, ADMIN_NAV_ITEM } from "./navConfig";

function NavItem({ to, label, icon, onNavigate }) {
  return (
    <li className="nav-item">
      <NavLink
        to={to}
        onClick={onNavigate}
        className={({ isActive }) =>
          `d-flex align-items-center gap-2 px-3 py-2 rounded-3 text-decoration-none small fw-medium vc-nav-link${isActive ? " active" : ""}`
        }
      >
        <i className={`bi ${icon}`} aria-hidden="true" />
        {label}
      </NavLink>
    </li>
  );
}

/**
 * Desktop: fixed-width column, always visible (md and up).
 * Mobile: off-canvas panel controlled by `open`/`onClose` from AppShell's hamburger toggle.
 */
export default function Sidebar({ open, onClose }) {
  const admin = useAuthStore(selectIsAdmin);

  const items = admin ? [...NAV_ITEMS, ADMIN_NAV_ITEM] : NAV_ITEMS;

  return (
    <>
      {open && (
        <div
          className="d-md-none position-fixed top-0 start-0 w-100 h-100"
          style={{ background: "rgba(15,23,42,0.5)", zIndex: 1040 }}
          onClick={onClose}
          aria-hidden="true"
        />
      )}
      <aside
        className={`vc-sidebar d-flex flex-column ${open ? "vc-sidebar-open" : ""}`}
        aria-label="Primary navigation"
      >
        <nav>
          <ul className="nav flex-column gap-1 list-unstyled px-2 pt-3">
            {items.map((item) => (
              <NavItem key={item.to} {...item} onNavigate={onClose} />
            ))}
          </ul>
        </nav>
      </aside>
    </>
  );
}
