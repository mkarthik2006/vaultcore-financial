import { useMemo, useState } from "react";
import { createTransfer } from "../../services/transferApi";

const STEPS = {
  FROM: 1,
  TO: 2,
  AMOUNT: 3,
  REVIEW: 4,
};

export default function TransferWizard() {
  const [step, setStep] = useState(STEPS.FROM);

  const [fromAccount, setFromAccount] = useState("A001");
  const [toAccount, setToAccount] = useState("");
  const [amount, setAmount] = useState("");
  const [currency, setCurrency] = useState("USD");
  const [remarks, setRemarks] = useState("");

  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const validation = useMemo(() => {
    const errors = {};
    if (!fromAccount || fromAccount.trim().length === 0) errors.fromAccount = "From account is required";
    if (!toAccount || toAccount.trim().length === 0) errors.toAccount = "Recipient account is required";
    if (fromAccount && toAccount && fromAccount.trim() === toAccount.trim()) {
      errors.toAccount = "Recipient must be different from sender";
    }

    const parsed = Number(amount);
    if (step >= STEPS.AMOUNT) {
      if (!amount || amount.trim().length === 0) errors.amount = "Amount is required";
      else if (Number.isNaN(parsed)) errors.amount = "Amount must be a number";
      else if (parsed <= 0) errors.amount = "Amount must be greater than 0";
    }

    if (!currency || currency.trim().length !== 3) {
      errors.currency = "Currency must be 3 letters (e.g., USD)";
    }
    return errors;
  }, [fromAccount, toAccount, amount, currency, step]);

  const canGoNext =
    (step === STEPS.FROM && !validation.fromAccount && !validation.currency) ||
    (step === STEPS.TO && !validation.toAccount && !validation.currency) ||
    (step === STEPS.AMOUNT && !validation.amount && !validation.currency) ||
    step === STEPS.REVIEW;

  async function onSubmit() {
    setSubmitting(true);
    setError(null);
    setResult(null);
    try {
      const payload = {
        fromAccount: fromAccount.trim(),
        toAccount: toAccount.trim(),
        amount: Number(amount),
        currency: currency.trim().toUpperCase(),
      };
      const res = await createTransfer(payload);
      setResult(res);
      setStep(STEPS.REVIEW);
    } catch (e) {
      setError(e?.message || "Transfer failed");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main style={{ maxWidth: 540, margin: "2rem auto", background: "#fff", borderRadius: 14, padding: "2rem" }}>
      <h3>Send Money Wizard</h3>

      {error && <div className="vault-error">Error: {error}</div>}
      {result && (
        <div style={{ background: "#e6faee", border: "1px solid #16a34a", color: "#166534", padding: 12, borderRadius: 8 }}>
          <div><strong>Transfer submitted</strong></div>
          <div>Transaction Ref: <code>{result.transactionReferenceId}</code></div>
          <div>Ledger Txn ID: <code>{result.ledgerTransactionId}</code></div>
        </div>
      )}

      <nav style={{ margin: "12px 0", display: "flex", gap: 6, flexWrap: "wrap" }}>
        {[STEPS.FROM, STEPS.TO, STEPS.AMOUNT, STEPS.REVIEW].map((s, i) => (
          <span
            key={s}
            style={{
              background: step === s ? "#0f172a" : "#e5e7eb",
              color: step === s ? "#fff" : "#334155",
              padding: "5px 14px",
              borderRadius: 20,
              fontSize: "0.9rem",
            }}
          >
            {["From", "To", "Amount", "Review"][i]}
          </span>
        ))}
      </nav>

      {step === STEPS.FROM && (
        <label className="vault-form-group">
          <span>From Account</span>
          <input value={fromAccount} onChange={(e) => setFromAccount(e.target.value)} placeholder="A001" />
          {validation.fromAccount && <div className="vault-error">{validation.fromAccount}</div>}
        </label>
      )}

      {step === STEPS.TO && (
        <label className="vault-form-group">
          <span>To Account</span>
          <input value={toAccount} onChange={(e) => setToAccount(e.target.value)} placeholder="A002" />
          {validation.toAccount && <div className="vault-error">{validation.toAccount}</div>}
        </label>
      )}

      {step === STEPS.AMOUNT && (
        <>
          <label className="vault-form-group">
            <span>Amount</span>
            <input value={amount} onChange={(e) => setAmount(e.target.value)} placeholder="200.00" />
            {validation.amount && <div className="vault-error">{validation.amount}</div>}
          </label>
          <label className="vault-form-group">
            <span>Currency</span>
            <input value={currency} onChange={(e) => setCurrency(e.target.value)} maxLength={3} placeholder="USD" />
            {validation.currency && <div className="vault-error">{validation.currency}</div>}
          </label>
          <label className="vault-form-group">
            <span>Remarks (optional UI only)</span>
            <input value={remarks} onChange={(e) => setRemarks(e.target.value)} placeholder="Optional note" />
          </label>
        </>
      )}

      {step === STEPS.REVIEW && (
        <div style={{ background: "#f6f8fa", borderRadius: 8, padding: 12 }}>
          <div><strong>From:</strong> {fromAccount}</div>
          <div><strong>To:</strong> {toAccount}</div>
          <div><strong>Amount:</strong> {amount}</div>
          <div><strong>Currency:</strong> {currency.toUpperCase()}</div>
          <div><strong>Remarks:</strong> {remarks || "-"}</div>
        </div>
      )}

      <nav style={{ display: "flex", gap: 10, marginTop: 16 }}>
        <button
          type="button"
          onClick={() => setStep((s) => Math.max(STEPS.FROM, s - 1))}
          disabled={submitting || step === STEPS.FROM}
          style={{ width: "auto", padding: "10px 14px", background: "#64748b" }}
        >
          Back
        </button>

        {step !== STEPS.REVIEW && (
          <button
            type="button"
            onClick={() => canGoNext && setStep((s) => s + 1)}
            disabled={submitting || !canGoNext}
            style={{ width: "auto", padding: "10px 14px" }}
          >
            Next
          </button>
        )}

        {step === STEPS.REVIEW && (
          <button
            type="button"
            onClick={onSubmit}
            disabled={submitting || !!result}
            style={{ width: "auto", padding: "10px 14px", background: "#16a34a" }}
          >
            {submitting ? "Submitting..." : result ? "Submitted" : "Confirm"}
          </button>
        )}
      </nav>
    </main>
  );
}