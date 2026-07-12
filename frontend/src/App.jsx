import { Suspense, lazy, useEffect, useState } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./components/LoginPage";
import ErrorBoundary from "./components/ErrorBoundary";
import AppShell from "./components/layout/AppShell";
import { ToastProvider } from "./components/ui/ToastProvider";
import NotFound from "./pages/errors/NotFound";
import Forbidden from "./pages/errors/Forbidden";
import { initAuth, refreshToken } from "./services/auth";
import { useAuthStore, selectIsAuthenticated, selectIsAdmin } from "./store/authStore";

// Route-level code splitting: these pages (Recharts, PDF download flow, etc.) are only needed
// once a user is authenticated, so keep them out of the initial (login-page) bundle.
const Dashboard = lazy(() => import("./pages/Dashboard"));
const SendMoney = lazy(() => import("./pages/SendMoney"));
const Portfolio = lazy(() => import("./pages/Portfolio"));
const Statements = lazy(() => import("./pages/Statements"));
const AdminProvisioning = lazy(() => import("./pages/AdminProvisioning"));

function PageFallback() {
  return (
    <div className="d-flex justify-content-center py-5">
      <div className="spinner-border" style={{ color: "var(--vc-brand)" }} role="status">
        <span className="visually-hidden">Loading…</span>
      </div>
    </div>
  );
}

function Protected({ children }) {
  return (
    <AppShell>
      <Suspense fallback={<PageFallback />}>{children}</Suspense>
    </AppShell>
  );
}

function App() {
  const [ready, setReady] = useState(false);

  // Reactive auth state from the Zustand store (populated by services/auth.js).
  const authenticated = useAuthStore(selectIsAuthenticated);
  const admin = useAuthStore(selectIsAdmin);

  useEffect(() => {
    initAuth()
      .then(() => setReady(true))
      .catch(() => setReady(true));

    const interval = setInterval(() => {
      refreshToken().catch(() => {});
    }, 60000);

    return () => clearInterval(interval);
  }, []);

  if (!ready) {
    return <PageFallback />;
  }

  return (
    <ErrorBoundary>
      <ToastProvider>
        <BrowserRouter>
          <Routes>
            <Route
              path="/"
              element={authenticated ? <Navigate to="/dashboard" replace /> : <LoginPage />}
            />

            <Route
              path="/dashboard"
              element={authenticated ? <Protected><Dashboard /></Protected> : <Navigate to="/" replace />}
            />

            <Route
              path="/send-money"
              element={authenticated ? <Protected><SendMoney /></Protected> : <Navigate to="/" replace />}
            />

            <Route
              path="/portfolio"
              element={authenticated ? <Protected><Portfolio /></Protected> : <Navigate to="/" replace />}
            />

            <Route
              path="/statements"
              element={authenticated ? <Protected><Statements /></Protected> : <Navigate to="/" replace />}
            />

            <Route
              path="/admin"
              element={
                !authenticated
                  ? <Navigate to="/" replace />
                  : admin
                    ? <Protected><AdminProvisioning /></Protected>
                    : <Forbidden />
              }
            />

            <Route path="*" element={authenticated ? <Protected><NotFound /></Protected> : <NotFound />} />
          </Routes>
        </BrowserRouter>
      </ToastProvider>
    </ErrorBoundary>
  );
}

export default App;
