import { useState } from "react";
import { createAccount, createUser } from "../services/adminApi";

export default function AdminProvisioning() {
  const [userForm, setUserForm] = useState({
    email: "",
    username: "",
    roles: "USER",
    enabled: true,
    passwordHash: ""
  });

  const [accountForm, setAccountForm] = useState({
    accountNumber: "",
    currency: "USD"
  });

  const [userResult, setUserResult] = useState(null);
  const [accountResult, setAccountResult] = useState(null);
  const [userError, setUserError] = useState("");
  const [accountError, setAccountError] = useState("");
  const [submittingUser, setSubmittingUser] = useState(false);
  const [submittingAccount, setSubmittingAccount] = useState(false);

  async function handleCreateUser(e) {
    e.preventDefault();
    setSubmittingUser(true);
    setUserError("");
    setUserResult(null);
    try {
      const payload = {
        email: userForm.email.trim(),
        username: userForm.username.trim(),
        roles: userForm.roles.trim(),
        enabled: !!userForm.enabled,
        passwordHash: userForm.passwordHash.trim()
      };
      const res = await createUser(payload);
      setUserResult(res);
      setUserForm({
        email: "",
        username: "",
        roles: "USER",
        enabled: true,
        passwordHash: ""
      });
    } catch (e) {
      setUserError(e?.message || "Failed to create user");
    } finally {
      setSubmittingUser(false);
    }
  }

  async function handleCreateAccount(e) {
    e.preventDefault();
    setSubmittingAccount(true);
    setAccountError("");
    setAccountResult(null);
    try {
      const payload = {
        accountNumber: accountForm.accountNumber.trim(),
        currency: accountForm.currency.trim().toUpperCase()
      };
      const res = await createAccount(payload);
      setAccountResult(res);
      setAccountForm({
        accountNumber: "",
        currency: "USD"
      });
    } catch (e) {
      setAccountError(e?.message || "Failed to create account");
    } finally {
      setSubmittingAccount(false);
    }
  }

  return (
    <main style={{
      maxWidth: 920,
      margin: "3rem auto",
      padding: "0 1rem"
    }}>
      <header style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "space-between",
        marginBottom: 24
      }}>
        <h2 style={{ color: "#0f172a" }}>Admin Provisioning</h2>
        <span style={{
          background: "#0f172a",
          color: "#fff",
          padding: "6px 12px",
          borderRadius: 999,
          fontSize: "0.85rem"
        }}>Authorized Use Only</span>
      </header>

      <section style={{
        display: "grid",
        gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))",
        gap: 20
      }}>
        <div style={{
          background: "#fff",
          borderRadius: 12,
          padding: "1.5rem",
          boxShadow: "0 8px 24px rgba(16, 29, 51, 0.08)"
        }}>
          <h3>Create User</h3>
          <p style={{ color: "#64748b", fontSize: "0.95rem" }}>
            Creates a backend user record (Keycloak user must already exist).
          </p>

          {userError && (
            <div style={{
              background: "#fee", border: "1px solid #dc2626",
              color: "#dc2626", padding: 10, borderRadius: 8, marginBottom: 12
            }}>
              <strong>Error:</strong> {userError}
            </div>
          )}

          {userResult && (
            <div style={{
              background: "#e6faee", border: "1px solid #16a34a",
              color: "#166534", padding: 10, borderRadius: 8, marginBottom: 12
            }}>
              ✅ User created: <strong>{userResult.username}</strong>
            </div>
          )}

          <form onSubmit={handleCreateUser} style={{ display: "grid", gap: 10 }}>
            <input
              placeholder="Email"
              value={userForm.email}
              onChange={e => setUserForm({ ...userForm, email: e.target.value })}
              required
            />
            <input
              placeholder="Username"
              value={userForm.username}
              onChange={e => setUserForm({ ...userForm, username: e.target.value })}
              required
            />
            <input
              placeholder="Roles (e.g., USER)"
              value={userForm.roles}
              onChange={e => setUserForm({ ...userForm, roles: e.target.value })}
            />
            <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
              <input
                type="checkbox"
                checked={userForm.enabled}
                onChange={e => setUserForm({ ...userForm, enabled: e.target.checked })}
              />
              Enabled
            </label>
            <input
              placeholder="Password Hash (optional)"
              value={userForm.passwordHash}
              onChange={e => setUserForm({ ...userForm, passwordHash: e.target.value })}
            />
            <button type="submit" disabled={submittingUser}>
              {submittingUser ? "Creating..." : "Create User"}
            </button>
          </form>
        </div>

        <div style={{
          background: "#fff",
          borderRadius: 12,
          padding: "1.5rem",
          boxShadow: "0 8px 24px rgba(16, 29, 51, 0.08)"
        }}>
          <h3>Create Account</h3>
          <p style={{ color: "#64748b", fontSize: "0.95rem" }}>
            Creates an account for ledger / transfers.
          </p>

          {accountError && (
            <div style={{
              background: "#fee", border: "1px solid #dc2626",
              color: "#dc2626", padding: 10, borderRadius: 8, marginBottom: 12
            }}>
              <strong>Error:</strong> {accountError}
            </div>
          )}

          {accountResult && (
            <div style={{
              background: "#e6faee", border: "1px solid #16a34a",
              color: "#166534", padding: 10, borderRadius: 8, marginBottom: 12
            }}>
              ✅ Account created: <strong>{accountResult.accountNumber}</strong>
            </div>
          )}

          <form onSubmit={handleCreateAccount} style={{ display: "grid", gap: 10 }}>
            <input
              placeholder="Account Number"
              value={accountForm.accountNumber}
              onChange={e => setAccountForm({ ...accountForm, accountNumber: e.target.value })}
              required
            />
            <input
              placeholder="Currency (e.g., USD)"
              value={accountForm.currency}
              onChange={e => setAccountForm({ ...accountForm, currency: e.target.value })}
              maxLength={3}
              required
            />
            <button type="submit" disabled={submittingAccount}>
              {submittingAccount ? "Creating..." : "Create Account"}
            </button>
          </form>
        </div>
      </section>

      <footer style={{
        marginTop: "2rem",
        textAlign: "center",
        fontSize: "0.9rem",
        color: "#64748b"
      }}>
        VaultCore &copy; 2026. Admin provisioning requires authorization.
      </footer>
    </main>
  );
}