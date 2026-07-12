/** Single source of truth for sidebar/nav items so TopNav (mobile) and Sidebar (desktop) stay in sync. */
export const NAV_ITEMS = [
  { to: "/dashboard", label: "Dashboard", icon: "bi-grid-1x2-fill" },
  { to: "/portfolio", label: "Portfolio", icon: "bi-pie-chart-fill" },
  { to: "/send-money", label: "Send Money", icon: "bi-send-fill" },
  { to: "/statements", label: "Statements", icon: "bi-file-earmark-text-fill" },
];

export const ADMIN_NAV_ITEM = { to: "/admin", label: "Admin", icon: "bi-shield-lock-fill" };
