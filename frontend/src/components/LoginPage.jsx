import { login } from "../services/auth";
import "./LoginPage.css";

export default function LoginPage() {
  return (
    <main className="vault-login-wrapper" role="main">
      <section className="vault-login-card">
        <img src="/vaultcore-logo.svg" alt="VaultCore Logo" style={{ width: 48, marginBottom: 16 }} />
        <h2 className="vault-brand">VaultCore</h2>
        <p className="vault-subtitle" id="subtitle">Sign in to securely access your financial dashboard</p>
        {/* Error message area, if you use one */}
        <div className="vault-error" aria-live="polite" style={{ display: 'none' }}>
          {/* dynamically show error here */}
        </div>
        <button onClick={login} style={{ marginTop: 16 }} tabIndex={0} aria-label="Login with Keycloak">
          <span role="img" aria-label="key">🔑</span>&nbsp; Login with Keycloak
        </button>
        <footer className="vault-footer">
          <a href="mailto:support@vaultcore.com">Support</a> | <a href="/privacy">Privacy</a>
          <div style={{ marginTop: 8, color: "#0f172a" }}>
            <small>Enterprise logins only — unauthorized access prohibited</small>
          </div>
        </footer>
      </section>
    </main>
  );
}