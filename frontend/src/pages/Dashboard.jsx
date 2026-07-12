import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuthStore, selectUsername, selectIsAdmin, selectSessionStartedAt } from "../store/authStore";
import { getPortfolio } from "../services/portfolioApi";
import { getBalance } from "../services/ledgerApi";
import { getSystemHealth } from "../services/healthApi";
import Card, { CardHeader } from "../components/ui/Card";
import StatusChip from "../components/ui/StatusChip";
import Button from "../components/ui/Button";
import Alert from "../components/ui/Alert";
import { SkeletonCard } from "../components/ui/Skeleton";
import PageHeader from "../components/ui/PageHeader";

const QUICK_ACTIONS = [
  { to: "/send-money", label: "Send Money", icon: "bi-send-fill", tone: "brand" },
  { to: "/portfolio", label: "View Portfolio", icon: "bi-pie-chart-fill", tone: "info" },
  { to: "/statements", label: "Statements", icon: "bi-file-earmark-text-fill", tone: "neutral" },
];

function formatMoney(value) {
  const n = Number(value ?? 0);
  return n.toLocaleString(undefined, { style: "currency", currency: "USD", maximumFractionDigits: 2 });
}

function useSystemHealth() {
  const [status, setStatus] = useState("checking");
  useEffect(() => {
    let cancelled = false;
    getSystemHealth()
      .then((s) => !cancelled && setStatus(s))
      .catch(() => !cancelled && setStatus("DOWN"));
    return () => { cancelled = true; };
  }, []);
  return status;
}

function PortfolioSummaryCard() {
  const [state, setState] = useState({ loading: true, error: "", data: null });

  useEffect(() => {
    let cancelled = false;
    getPortfolio()
      .then((data) => !cancelled && setState({ loading: false, error: "", data }))
      .catch((e) => !cancelled && setState({ loading: false, error: e.message || "Failed to load portfolio", data: null }));
    return () => { cancelled = true; };
  }, []);

  return (
    <Card className="h-100">
      <CardHeader title="Portfolio Value" icon="bi-pie-chart-fill" />
      {state.loading && <SkeletonCard lines={1} />}
      {!state.loading && state.error && <Alert tone="danger">{state.error}</Alert>}
      {!state.loading && !state.error && (
        <>
          <div className="fs-3 fw-bold" style={{ color: "var(--vc-text)" }}>
            {formatMoney(state.data?.totalValue)}
          </div>
          <p className="small vc-text-muted mb-3">
            {state.data?.holdings?.length ?? 0} holding{(state.data?.holdings?.length ?? 0) === 1 ? "" : "s"}
          </p>
          <Link to="/portfolio" className="small fw-semibold text-decoration-none">
            View portfolio <i className="bi bi-arrow-right" aria-hidden="true" />
          </Link>
        </>
      )}
    </Card>
  );
}

function SecuritySessionCard() {
  const admin = useAuthStore(selectIsAdmin);
  const sessionStartedAt = useAuthStore(selectSessionStartedAt);
  const health = useSystemHealth();

  const healthTone = health === "UP" ? "success" : health === "checking" ? "neutral" : "danger";
  const healthLabel = health === "checking" ? "Checking…" : health === "UP" ? "All systems operational" : "System unavailable";

  return (
    <Card className="h-100">
      <CardHeader title="Security &amp; Session" icon="bi-shield-check" />
      <ul className="list-unstyled d-flex flex-column gap-2 mb-0 small">
        <li className="d-flex justify-content-between align-items-center">
          <span className="vc-text-muted">Role</span>
          <StatusChip tone={admin ? "brand" : "neutral"} icon={admin ? "bi-shield-lock-fill" : "bi-person-fill"}>
            {admin ? "Administrator" : "Standard User"}
          </StatusChip>
        </li>
        <li className="d-flex justify-content-between align-items-center">
          <span className="vc-text-muted">Authentication</span>
          <StatusChip tone="success" icon="bi-lock-fill">Keycloak / OIDC</StatusChip>
        </li>
        <li className="d-flex justify-content-between align-items-center">
          <span className="vc-text-muted">Session started</span>
          <span className="fw-medium">
            {sessionStartedAt ? new Date(sessionStartedAt).toLocaleTimeString() : "—"}
          </span>
        </li>
        <li className="d-flex justify-content-between align-items-center">
          <span className="vc-text-muted">System status</span>
          <StatusChip tone={healthTone} icon={health === "UP" ? "bi-check-circle-fill" : "bi-exclamation-circle-fill"}>
            {healthLabel}
          </StatusChip>
        </li>
      </ul>
    </Card>
  );
}

function QuickActionsCard() {
  const admin = useAuthStore(selectIsAdmin);
  const actions = admin ? [...QUICK_ACTIONS, { to: "/admin", label: "Admin Provisioning", icon: "bi-shield-lock-fill", tone: "warning" }] : QUICK_ACTIONS;

  return (
    <Card className="h-100">
      <CardHeader title="Quick Actions" icon="bi-lightning-charge-fill" />
      <div className="d-flex flex-column gap-2">
        {actions.map((a) => (
          <Link
            key={a.to}
            to={a.to}
            className="d-flex align-items-center justify-content-between px-3 py-2 rounded-3 text-decoration-none vc-surface-muted"
            style={{ color: "var(--vc-text)" }}
          >
            <span className="d-flex align-items-center gap-2 small fw-medium">
              <i className={`bi ${a.icon}`} aria-hidden="true" />
              {a.label}
            </span>
            <i className="bi bi-chevron-right small vc-text-muted" aria-hidden="true" />
          </Link>
        ))}
      </div>
    </Card>
  );
}

function BalanceLookupCard() {
  const [accountNumber, setAccountNumber] = useState("");
  const [currency, setCurrency] = useState("USD");
  const [state, setState] = useState({ loading: false, error: "", balance: null });

  async function handleCheck(e) {
    e.preventDefault();
    setState({ loading: true, error: "", balance: null });
    try {
      const balance = await getBalance(accountNumber.trim(), currency.trim().toUpperCase());
      setState({ loading: false, error: "", balance });
    } catch (err) {
      setState({ loading: false, error: err.message || "Failed to fetch balance", balance: null });
    }
  }

  return (
    <Card>
      <CardHeader
        title="Check Account Balance"
        subtitle="Look up the live ledger balance for any account you own"
        icon="bi-wallet2"
      />
      <form className="row g-2 align-items-end" onSubmit={handleCheck}>
        <div className="col-12 col-sm-5">
          <label htmlFor="balance-account" className="form-label small vc-text-muted mb-1">Account number</label>
          <input
            id="balance-account"
            className="form-control"
            placeholder="e.g. ACC-TEST-0001"
            value={accountNumber}
            onChange={(e) => setAccountNumber(e.target.value)}
            required
          />
        </div>
        <div className="col-8 col-sm-3">
          <label htmlFor="balance-currency" className="form-label small vc-text-muted mb-1">Currency</label>
          <input
            id="balance-currency"
            className="form-control text-uppercase"
            placeholder="USD"
            maxLength={3}
            value={currency}
            onChange={(e) => setCurrency(e.target.value)}
            required
          />
        </div>
        <div className="col-4 col-sm-4">
          <Button type="submit" busy={state.loading} busyLabel="Checking…" icon="bi-search" className="w-100 justify-content-center">
            Check
          </Button>
        </div>
      </form>

      {state.error && <Alert tone="danger" className="mt-3">{state.error}</Alert>}
      {state.balance !== null && !state.error && (
        <div className="mt-3 p-3 rounded-3 vc-surface-muted d-flex justify-content-between align-items-center">
          <span className="small vc-text-muted">Available balance</span>
          <span className="fs-5 fw-bold" style={{ color: "var(--vc-text)" }}>
            {Number(state.balance).toLocaleString(undefined, { style: "currency", currency: currency.toUpperCase() || "USD" })}
          </span>
        </div>
      )}
    </Card>
  );
}

export default function Dashboard() {
  const username = useAuthStore(selectUsername);

  return (
    <div className="vc-fade-in">
      <PageHeader
        title="Dashboard"
        description={`Welcome back, ${username || "there"}. Here's your account overview.`}
      />

      <div className="row g-3 mb-3">
        <div className="col-12 col-md-4"><PortfolioSummaryCard /></div>
        <div className="col-12 col-md-4"><SecuritySessionCard /></div>
        <div className="col-12 col-md-4"><QuickActionsCard /></div>
      </div>

      <div className="row g-3">
        <div className="col-12">
          <BalanceLookupCard />
        </div>
      </div>
    </div>
  );
}
