import { login } from "../services/auth";

export default function LoginPage() {
  return (
    <div style={{ maxWidth: 360, margin: "4rem auto" }}>
      <h2>VaultCore Login</h2>
      <button onClick={login} style={{ marginTop: 16 }}>
        Login with Keycloak
      </button>
    </div>
  );
}