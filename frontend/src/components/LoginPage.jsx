import { useState } from "react";
import { login } from "../services/auth";
import Button from "./ui/Button";

export default function LoginPage() {
  const [redirecting, setRedirecting] = useState(false);

  function handleLogin() {
    setRedirecting(true);
    login();
  }

  return (
    <main className="vc-auth-backdrop d-flex align-items-center justify-content-center px-3" role="main">
      <section className="vc-card p-4 p-md-5 vc-fade-in" style={{ maxWidth: 400, width: "100%" }}>
        <div className="text-center mb-4">
          <img src="/vaultcore-logo.svg" alt="VaultCore Logo" width={48} height={48} className="mb-3" />
          <h1 className="h4 fw-bold mb-1" style={{ color: "var(--vc-text)" }}>VaultCore</h1>
          <p className="small vc-text-muted mb-0" id="subtitle">
            Sign in to securely access your financial dashboard
          </p>
        </div>

        <div className="vc-error" aria-live="polite" style={{ display: "none" }} />

        <Button
          onClick={handleLogin}
          busy={redirecting}
          busyLabel="Redirecting to sign-in…"
          icon="bi-shield-lock-fill"
          className="w-100 justify-content-center py-2"
          tabIndex={0}
          aria-label="Login with Keycloak"
        >
          Login with Keycloak
        </Button>

        <footer className="text-center mt-4 small vc-text-muted">
          <a href="mailto:support@vaultcore.com" className="text-decoration-none">Support</a>
          {" "}&middot;{" "}
          <a href="/privacy" className="text-decoration-none">Privacy</a>
          <div className="mt-2" style={{ color: "var(--vc-text)" }}>
            Enterprise logins only &mdash; unauthorized access prohibited
          </div>
        </footer>
      </section>
    </main>
  );
}
