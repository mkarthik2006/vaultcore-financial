import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { login } from "../services/auth";
import "./LoginPage.css";

export default function LoginPage() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      await login(username, password);
      navigate("/dashboard");
    } catch (err) {
      setError(err.message || "Authentication failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="vault-login-wrapper">
      <div className="vault-login-card">
        <h1 className="vault-brand">VaultCore Financial</h1>
        <p className="vault-subtitle">Secure Digital Banking Portal</p>

        <form onSubmit={handleLogin} className="vault-form">
          <div className="vault-form-group">
            <label htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              value={username}
              placeholder="Enter your username"
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>

          <div className="vault-form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              placeholder="Enter your password"
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          {error && <div className="vault-error">{error}</div>}

          <button type="submit" disabled={loading}>
            {loading ? "Authenticating..." : "Login Securely"}
          </button>
        </form>

        <div className="vault-footer">
          <p>🔒 Protected by JWT Authentication & Enterprise Security Standards</p>
          <a href="#">Forgot Password?</a>
        </div>
      </div>
    </div>
  );
}
