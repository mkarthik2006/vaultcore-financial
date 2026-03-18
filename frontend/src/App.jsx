import { useEffect, useState } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./components/LoginPage";
import Dashboard from "./components/Dashboard";
import SendMoney from "./pages/SendMoney";
import Portfolio from "./pages/Portfolio";
import AdminProvisioning from "./pages/AdminProvisioning";
import { initAuth, isAuthenticated, refreshToken } from "./services/auth";

function App() {
  const [ready, setReady] = useState(false);

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
    <BrowserRouter>
      <Routes>
        <Route
          path="/"
          element={
            isAuthenticated()
              ? <Navigate to="/dashboard" replace />
              : <LoginPage />
          }
        />

        <Route
          path="/dashboard"
          element={
            isAuthenticated()
              ? <Dashboard />
              : <Navigate to="/" replace />
          }
        />

        <Route
          path="/send-money"
          element={
            isAuthenticated()
              ? <SendMoney />
              : <Navigate to="/" replace />
          }
        />

        <Route
          path="/portfolio"
          element={
            isAuthenticated()
              ? <Portfolio />
              : <Navigate to="/" replace />
          }
        />

        <Route
          path="/admin"
          element={
            isAuthenticated()
              ? <AdminProvisioning />
              : <Navigate to="/" replace />
          }
        />

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;