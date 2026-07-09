import { useEffect, useState } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./components/LoginPage";
import Dashboard from "./components/Dashboard";
import ErrorBoundary from "./components/ErrorBoundary";
import SendMoney from "./pages/SendMoney";
import Portfolio from "./pages/Portfolio";
import AdminProvisioning from "./pages/AdminProvisioning";
import { initAuth, refreshToken } from "./services/auth";
import { useAuthStore, selectIsAuthenticated, selectIsAdmin } from "./store/authStore";

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
    return <div style={{ padding: "2rem" }}>Loading...</div>;
  }

  return (
    <ErrorBoundary>
      <BrowserRouter>
        <Routes>
          <Route
            path="/"
            element={authenticated ? <Navigate to="/dashboard" replace /> : <LoginPage />}
          />

          <Route
            path="/dashboard"
            element={authenticated ? <Dashboard /> : <Navigate to="/" replace />}
          />

          <Route
            path="/send-money"
            element={authenticated ? <SendMoney /> : <Navigate to="/" replace />}
          />

          <Route
            path="/portfolio"
            element={authenticated ? <Portfolio /> : <Navigate to="/" replace />}
          />

          <Route
            path="/admin"
            element={
              !authenticated
                ? <Navigate to="/" replace />
                : admin
                  ? <AdminProvisioning />
                  : <Navigate to="/dashboard" replace />
            }
          />

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </ErrorBoundary>
  );
}

export default App;
