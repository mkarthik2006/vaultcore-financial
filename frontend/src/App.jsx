import { useEffect, useState } from "react";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import LoginPage from "./components/LoginPage";
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

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;