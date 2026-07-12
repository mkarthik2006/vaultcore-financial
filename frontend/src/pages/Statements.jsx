import { useState } from "react";
import { downloadMonthlyStatement } from "../services/statementsApi";
import Card, { CardHeader } from "../components/ui/Card";
import PageHeader from "../components/ui/PageHeader";
import Alert from "../components/ui/Alert";
import Button from "../components/ui/Button";
import EmptyState from "../components/ui/EmptyState";
import { useToast } from "../components/ui/toastContext";

function currentMonthValue() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
}

export default function Statements() {
  const toast = useToast();
  const [accountNumber, setAccountNumber] = useState("");
  const [month, setMonth] = useState(currentMonthValue());
  const [state, setState] = useState({ loading: false, error: "", lastDownload: null });

  async function handleDownload(e) {
    e.preventDefault();
    setState({ loading: true, error: "", lastDownload: null });
    try {
      const { filename } = await downloadMonthlyStatement(accountNumber.trim(), month);
      setState({ loading: false, error: "", lastDownload: filename });
      toast.push(`Downloaded ${filename}`, { tone: "success" });
    } catch (err) {
      setState({ loading: false, error: err.message || "Failed to generate statement", lastDownload: null });
    }
  }

  return (
    <div className="vc-fade-in">
      <PageHeader
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Statements" }]}
        title="Statements"
        description="Generate and download a PDF statement for any account you own."
      />

      <Card style={{ maxWidth: 560 }}>
        <CardHeader title="Monthly Statement" icon="bi-file-earmark-pdf-fill" />
        <form onSubmit={handleDownload} className="row g-3 align-items-end">
          <div className="col-12 col-sm-6">
            <label htmlFor="stmt-account" className="form-label small vc-text-muted mb-1">Account number</label>
            <input
              id="stmt-account"
              className="form-control"
              placeholder="ACC-0001"
              value={accountNumber}
              onChange={(e) => setAccountNumber(e.target.value)}
              required
            />
          </div>
          <div className="col-12 col-sm-6">
            <label htmlFor="stmt-month" className="form-label small vc-text-muted mb-1">Month</label>
            <input
              id="stmt-month"
              type="month"
              className="form-control"
              value={month}
              onChange={(e) => setMonth(e.target.value)}
              required
            />
          </div>
          <div className="col-12">
            <Button type="submit" busy={state.loading} busyLabel="Generating…" icon="bi-download" className="w-100 justify-content-center">
              Download PDF Statement
            </Button>
          </div>
        </form>

        {state.error && <Alert tone="danger" className="mt-3">{state.error}</Alert>}
        {state.lastDownload && !state.error && (
          <Alert tone="success" className="mt-3">
            <span className="vc-mono">{state.lastDownload}</span> was downloaded to your device.
          </Alert>
        )}
        {!state.loading && !state.error && !state.lastDownload && (
          <EmptyState
            icon="bi-file-earmark-text"
            title="No statement generated yet"
            description="Enter an account number and month, then download the PDF."
          />
        )}
      </Card>
    </div>
  );
}
