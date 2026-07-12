import { useState } from "react";
import TopNav from "./TopNav";
import Sidebar from "./Sidebar";
import Footer from "./Footer";
import OfflineBanner from "./OfflineBanner";

/** Authenticated-area shell: top nav + responsive sidebar + main content + footer. */
export default function AppShell({ children }) {
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="vc-app-shell d-flex flex-column min-vh-100">
      <a href="#vc-main-content" className="vc-skip-link">Skip to main content</a>
      <OfflineBanner />
      <TopNav onToggleSidebar={() => setSidebarOpen((v) => !v)} />
      <div className="d-flex flex-grow-1">
        <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
        <div className="vc-content d-flex flex-column">
          <main id="vc-main-content" className="vc-main-scroll flex-grow-1 p-3 p-md-4" tabIndex={-1}>
            {children}
          </main>
          <Footer />
        </div>
      </div>
    </div>
  );
}
