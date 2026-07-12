import { useState } from "react";
import { createAccount, createUser } from "../services/adminApi";
import Card, { CardHeader } from "../components/ui/Card";
import PageHeader from "../components/ui/PageHeader";
import Alert from "../components/ui/Alert";
import Button from "../components/ui/Button";
import StatusChip from "../components/ui/StatusChip";
import EmptyState from "../components/ui/EmptyState";
import { useToast } from "../components/ui/toastContext";

function Field({ id, label, children }) {
  return (
    <div className="mb-3">
      <label htmlFor={id} className="form-label small vc-text-muted mb-1">{label}</label>
      {children}
    </div>
  );
}

export default function AdminProvisioning() {
  const toast = useToast();

  const [userForm, setUserForm] = useState({ email: "", username: "", roles: "USER", enabled: true, passwordHash: "" });
  const [accountForm, setAccountForm] = useState({ accountNumber: "", currency: "USD", ownerUsername: "" });

  const [userError, setUserError] = useState("");
  const [accountError, setAccountError] = useState("");
  const [submittingUser, setSubmittingUser] = useState(false);
  const [submittingAccount, setSubmittingAccount] = useState(false);

  // Session-local activity log — reflects real actions taken in this browser session, not
  // fabricated data. The backend exposes no list/search endpoint for users or accounts, so this
  // is intentionally not a persistent admin table.
  const [activity, setActivity] = useState([]);

  async function handleCreateUser(e) {
    e.preventDefault();
    setSubmittingUser(true);
    setUserError("");
    try {
      const payload = {
        email: userForm.email.trim(),
        username: userForm.username.trim(),
        roles: userForm.roles.trim(),
        enabled: !!userForm.enabled,
        passwordHash: userForm.passwordHash.trim(),
      };
      const res = await createUser(payload);
      toast.push(`User "${res.username}" created`, { tone: "success" });
      setActivity((prev) => [{ type: "user", label: res.username, detail: res.email, at: new Date() }, ...prev]);
      setUserForm({ email: "", username: "", roles: "USER", enabled: true, passwordHash: "" });
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
    try {
      const payload = {
        accountNumber: accountForm.accountNumber.trim(),
        currency: accountForm.currency.trim().toUpperCase(),
        ...(accountForm.ownerUsername.trim() ? { ownerUsername: accountForm.ownerUsername.trim() } : {}),
      };
      const res = await createAccount(payload);
      toast.push(`Account "${res.accountNumber}" created`, { tone: "success" });
      setActivity((prev) => [{ type: "account", label: res.accountNumber, detail: res.currency, at: new Date() }, ...prev]);
      setAccountForm({ accountNumber: "", currency: "USD", ownerUsername: "" });
    } catch (e) {
      setAccountError(e?.message || "Failed to create account");
    } finally {
      setSubmittingAccount(false);
    }
  }

  return (
    <div className="vc-fade-in">
      <PageHeader
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Admin" }]}
        title="Admin Provisioning"
        description="Create backend user and account records. Keycloak identities must already exist."
        actions={<StatusChip tone="warning" icon="bi-shield-lock-fill">Authorized Use Only</StatusChip>}
      />

      <div className="row g-3">
        <div className="col-12 col-lg-6">
          <Card>
            <CardHeader title="Create User" subtitle="Requires an existing Keycloak identity" icon="bi-person-plus-fill" />
            {userError && <Alert tone="danger" className="mb-3">{userError}</Alert>}
            <form onSubmit={handleCreateUser}>
              <Field id="user-email" label="Email">
                <input id="user-email" type="email" className="form-control" value={userForm.email} onChange={(e) => setUserForm({ ...userForm, email: e.target.value })} required />
              </Field>
              <Field id="user-username" label="Username">
                <input id="user-username" className="form-control" value={userForm.username} onChange={(e) => setUserForm({ ...userForm, username: e.target.value })} required />
              </Field>
              <Field id="user-roles" label="Roles">
                <input id="user-roles" className="form-control" placeholder="USER" value={userForm.roles} onChange={(e) => setUserForm({ ...userForm, roles: e.target.value })} />
              </Field>
              <div className="form-check mb-3">
                <input id="user-enabled" type="checkbox" className="form-check-input" checked={userForm.enabled} onChange={(e) => setUserForm({ ...userForm, enabled: e.target.checked })} />
                <label htmlFor="user-enabled" className="form-check-label small">Enabled</label>
              </div>
              <Field id="user-password" label="Password hash (optional)">
                <input id="user-password" type="password" className="form-control" autoComplete="new-password" value={userForm.passwordHash} onChange={(e) => setUserForm({ ...userForm, passwordHash: e.target.value })} />
              </Field>
              <Button type="submit" busy={submittingUser} busyLabel="Creating…" icon="bi-person-plus-fill" className="justify-content-center w-100">
                Create User
              </Button>
            </form>
          </Card>
        </div>

        <div className="col-12 col-lg-6">
          <Card>
            <CardHeader title="Create Account" subtitle="For ledger transfers and balances" icon="bi-bank2" />
            {accountError && <Alert tone="danger" className="mb-3">{accountError}</Alert>}
            <form onSubmit={handleCreateAccount}>
              <Field id="acct-number" label="Account number">
                <input id="acct-number" className="form-control" value={accountForm.accountNumber} onChange={(e) => setAccountForm({ ...accountForm, accountNumber: e.target.value })} required />
              </Field>
              <Field id="acct-currency" label="Currency">
                <input id="acct-currency" className="form-control text-uppercase" maxLength={3} value={accountForm.currency} onChange={(e) => setAccountForm({ ...accountForm, currency: e.target.value })} required />
              </Field>
              <Field id="acct-owner" label="Owner username (optional)">
                <input id="acct-owner" className="form-control" placeholder="Leave blank for an unowned / clearing account" value={accountForm.ownerUsername} onChange={(e) => setAccountForm({ ...accountForm, ownerUsername: e.target.value })} />
              </Field>
              <Button type="submit" busy={submittingAccount} busyLabel="Creating…" icon="bi-bank2" className="justify-content-center w-100">
                Create Account
              </Button>
            </form>
          </Card>
        </div>

        <div className="col-12">
          <Card>
            <CardHeader title="Session Activity" subtitle="Records created in this browser session" icon="bi-clock-history" />
            {activity.length === 0 ? (
              <EmptyState icon="bi-clock-history" title="No activity yet" description="Records you create above will appear here." />
            ) : (
              <ul className="list-unstyled d-flex flex-column gap-2 mb-0">
                {activity.map((a, i) => (
                  <li key={i} className="d-flex justify-content-between align-items-center small border-bottom pb-2">
                    <span className="d-flex align-items-center gap-2">
                      <StatusChip tone={a.type === "user" ? "info" : "neutral"} icon={a.type === "user" ? "bi-person-fill" : "bi-bank2"}>
                        {a.type === "user" ? "User" : "Account"}
                      </StatusChip>
                      <span className="fw-medium">{a.label}</span>
                      <span className="vc-text-muted">{a.detail}</span>
                    </span>
                    <span className="vc-text-muted">{a.at.toLocaleTimeString()}</span>
                  </li>
                ))}
              </ul>
            )}
          </Card>
        </div>
      </div>
    </div>
  );
}
