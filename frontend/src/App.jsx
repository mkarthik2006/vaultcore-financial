import { Suspense, lazy, useEffect, useState } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./components/LoginPage";
<<<<<<< HEAD
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
=======
import Dashboard from "./components/Dashboard";
import SendMoney from "./pages/SendMoney";
import TransferPage from "./pages/TransferPage";
import Portfolio from "./pages/Portfolio";
import AdminProvisioning from "./pages/AdminProvisioning";
import AdminUsersPage from "./pages/AdminUsersPage";
import AdminAccountsPage from "./pages/AdminAccountsPage";
import StatementsPage from "./pages/StatementsPage";
import AuditPage from "./pages/AuditPage";
import UnauthorizedPage from "./pages/UnauthorizedPage";
import ProtectedRoute from "./components/common/ProtectedRoute";
import { initAuth, isAuthenticated, refreshToken } from "./services/auth";
>>>>>>> origin/main

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
<<<<<<< HEAD
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
=======
    <BrowserRouter>
      <Routes>
        <Route path="/" element={isAuthenticated() ? <Navigate to="/dashboard" replace /> : <Navigate to="/login" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />

        <Route path="/dashboard" element={<ProtectedRoute><Dashboard /></ProtectedRoute>} />
        <Route path="/send-money" element={<ProtectedRoute><SendMoney /></ProtectedRoute>} />
        <Route path="/transfer" element={<ProtectedRoute><TransferPage /></ProtectedRoute>} />
        <Route path="/portfolio" element={<ProtectedRoute><Portfolio /></ProtectedRoute>} />

        <Route path="/admin" element={<ProtectedRoute><AdminProvisioning /></ProtectedRoute>} />
        <Route path="/admin/users" element={<ProtectedRoute><AdminUsersPage /></ProtectedRoute>} />
        <Route path="/admin/accounts" element={<ProtectedRoute><AdminAccountsPage /></ProtectedRoute>} />

        <Route path="/statements" element={<ProtectedRoute><StatementsPage /></ProtectedRoute>} />
        <Route path="/audit" element={<ProtectedRoute><AuditPage /></ProtectedRoute>} />
>>>>>>> origin/main

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
