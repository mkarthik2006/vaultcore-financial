import React, { useMemo, useState } from "react";
import { createTransfer } from "../services/transferApi";

const STEPS = {
  RECIPIENT: 1,
  AMOUNT: 2,
  CONFIRM: 3,
};

export default function SendMoney() {
  const [step, setStep] = useState(STEPS.RECIPIENT);

  const [fromAccount, setFromAccount] = useState("A001");
  const [toAccount, setToAccount] = useState("");
  const [amount, setAmount] = useState("");
  const [currency, setCurrency] = useState("USD");

  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const validation = useMemo(() => {
    const errors = {};
    if (!fromAccount || fromAccount.trim().length === 0) {
      errors.fromAccount = "From account is required";
    }
    if (!toAccount || toAccount.trim().length === 0) {
      errors.toAccount = "Recipient account is required";
    }
    if (fromAccount && toAccount && fromAccount.trim() === toAccount.trim()) {
      errors.toAccount = "Recipient must be different from sender";
    }
    const parsed = Number(amount);
    if (step >= STEPS.AMOUNT) {
      if (!amount || amount.trim().length === 0) {
        errors.amount = "Amount is required";
      } else if (Number.isNaN(parsed)) {
        errors.amount = "Amount must be a number";
      } else if (parsed <= 0) {
        errors.amount = "Amount must be greater than 0";
      }
    }
    if (!currency || currency.trim().length !== 3) {
      errors.currency = "Currency must be 3 letters (e.g., USD)";
    }
    return errors;
  }, [fromAccount, toAccount, amount, currency, step]);

  const canGoNext =
    (step === STEPS.RECIPIENT && !validation.fromAccount && !validation.toAccount && !validation.currency) ||
    (step === STEPS.AMOUNT && !validation.amount && !validation.currency) ||
    step === STEPS.CONFIRM;

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
      setStep(STEPS.CONFIRM);
    } catch (e) {
      setError(e?.message || "Transfer failed");
    } finally {
      setSubmitting(false);
    }
  }

  // Compliance visual refinement starts here, logic untouched
  return (
    <main style={{
      maxWidth: 540,
      margin: "3rem auto",
      background: "#fff",
      borderRadius: "14px",
      boxShadow: "0 8px 32px rgba(16, 29, 51, 0.13)",
      padding: "2.5rem 2rem",
      fontFamily: "system-ui,sans-serif"
    }}>
      <header style={{ marginBottom: 24, textAlign: "center" }}>
        <span style={{
          display: "inline-block",
          background: "linear-gradient(90deg,#0f172a,#334155)",
          color: "#fff", borderRadius: "2rem", fontWeight: 600,
          fontSize: "1.3rem", padding: "0.2em 1.1em", letterSpacing: "0.03em"
        }}>Send Money</span>
      </header>

      {/* Error status area */}
      {error && (
        <div className="vault-error" aria-live="polite" style={{
          background: "#fee", border: "1px solid #dc2626",
          color: "#dc2626", padding: 13, marginBottom: 18, borderRadius: 8
        }}>
          <strong>Error:</strong> {error}
        </div>
      )}

      {/* Transfer confirmation/success status */}
      {result && (
        <div className="vault-success" aria-live="polite" style={{
          background: "#e6faee", border: "1px solid #16a34a",
          color: "#166534", padding: 13, marginBottom: 18, borderRadius: 8
        }}>
          <div><strong>Transfer submitted</strong></div>
          <div>Transaction Ref: <code>{result.transactionReferenceId}</code></div>
          <div>Ledger Txn ID: <code>{result.ledgerTransactionId}</code></div>
        </div>
      )}

      {/* Step progress bar */}
      <nav aria-label="Progress steps" style={{ marginBottom: 18, display: 'flex', gap: 4, justifyContent: 'center' }}>
        {[STEPS.RECIPIENT, STEPS.AMOUNT, STEPS.CONFIRM].map((s, i) => (
          <span key={s} style={{
            background: step === s ? "#0f172a" : "#e5e7eb",
            color: step === s ? "#fff" : "#334155",
            padding: "5px 19px", borderRadius: "20px", fontWeight: 500,
            fontSize: "0.97rem", transition: "all .18s"
          }}>
            {["Recipient", "Amount", "Confirm"][i]}
          </span>
        ))}
      </nav>

      <section aria-live="polite">
        {step === STEPS.RECIPIENT && (
          <>
            <label className="vault-form-group">
              <span>From Account</span>
              <input
                value={fromAccount}
                onChange={e => setFromAccount(e.target.value)}
                placeholder="A001"
                className="vault-input"
                style={{
                  display: "block", width: "100%", padding: "11px",
                  borderRadius: "7px", marginTop: 5, border: "1px solid #cbd5e1", fontSize: "15px"
                }}
                aria-label="Sender Account Number"
              />
              {validation.fromAccount && <div className="vault-error">{validation.fromAccount}</div>}
            </label>
            <label className="vault-form-group" style={{ marginTop: 15 }}>
              <span>Recipient Account</span>
              <input
                value={toAccount}
                onChange={e => setToAccount(e.target.value)}
                placeholder="A002"
                className="vault-input"
                style={{
                  display: "block", width: "100%", padding: "11px",
                  borderRadius: "7px", marginTop: 5, border: "1px solid #cbd5e1", fontSize: "15px"
                }}
                aria-label="Recipient Account Number"
              />
              {validation.toAccount && <div className="vault-error">{validation.toAccount}</div>}
            </label>
            <label className="vault-form-group" style={{ marginTop: 15 }}>
              <span>Currency</span>
              <input
                value={currency}
                onChange={e => setCurrency(e.target.value)}
                placeholder="USD"
                className="vault-input"
                style={{
                  display: "block", width: "100%", padding: "11px",
                  borderRadius: "7px", marginTop: 5,
                  border: "1px solid #cbd5e1", textTransform: "uppercase", fontSize: "15px"
                }}
                aria-label="Currency Code"
                maxLength={3}
              />
              {validation.currency && <div className="vault-error">{validation.currency}</div>}
            </label>
          </>
        )}

        {step === STEPS.AMOUNT && (
          <>
            <div style={{
              marginBottom: 12,
              background: "#f6f8fa", padding: 13, borderRadius: 7, color: "#334155", fontSize: "0.98rem"
            }}>
              <div><strong>From:</strong> {fromAccount}</div>
              <div><strong>To:</strong> {toAccount}</div>
              <div><strong>Currency:</strong> {currency.toUpperCase()}</div>
            </div>
            <label className="vault-form-group">
              <span>Amount</span>
              <input
                value={amount}
                onChange={e => setAmount(e.target.value)}
                placeholder="200.00"
                inputMode="decimal"
                className="vault-input"
                style={{
                  display: "block", width: "100%", padding: "11px",
                  borderRadius: "7px", marginTop: 5, border: "1px solid #cbd5e1", fontSize: "15px"
                }}
                aria-label="Transfer Amount"
              />
              {validation.amount && <div className="vault-error">{validation.amount}</div>}
            </label>
          </>
        )}

        {step === STEPS.CONFIRM && (
          <>
            <div style={{
              marginBottom: 12,
              background: "#f6f8fa", padding: 13, borderRadius: 7, color: "#334155", fontSize: "0.98rem"
            }}>
              <div><strong>From:</strong> {fromAccount}</div>
              <div><strong>To:</strong> {toAccount}</div>
              <div><strong>Amount:</strong> {amount}</div>
              <div><strong>Currency:</strong> {currency.toUpperCase()}</div>
            </div>
            {!result && (
              <div style={{
                background: "#f7f7f7", padding: 11, border: "1px solid #ddd", borderRadius: 7,
                color: "#64748b", textAlign: "center"
              }}>
                Please review the transfer details &mdash; click Submit to confirm.
              </div>
            )}
          </>
        )}
      </section>

      {/* Navigation buttons */}
      <nav style={{
        display: "flex", gap: 13, marginTop: 20, justifyContent: "center"
      }}>
        <button
          type="button"
          onClick={() => setStep(s => Math.max(STEPS.RECIPIENT, s - 1))}
          disabled={submitting || step === STEPS.RECIPIENT}
          style={{
            padding: "13px 21px", fontWeight: 600, borderRadius: "7px", border: "none", background: "#64748b",
            color: "#fff", cursor: submitting || step === STEPS.RECIPIENT ? "not-allowed" : "pointer",
            opacity: submitting || step === STEPS.RECIPIENT ? 0.5 : 1, transition: "all .18s"
          }}
        >
          Back
        </button>
        {step !== STEPS.CONFIRM && (
          <button
            type="button"
            onClick={() => canGoNext && setStep(s => s + 1)}
            disabled={submitting || !canGoNext}
            style={{
              padding: "13px 21px", fontWeight: 600, borderRadius: "7px", border: "none", background: "#0f172a",
              color: "#fff", cursor: submitting || !canGoNext ? "not-allowed" : "pointer",
              opacity: submitting || !canGoNext ? 0.5 : 1, transition: "all .18s"
            }}
          >
            Next
          </button>
        )}
        {step === STEPS.CONFIRM && (
          <button
            type="button"
            onClick={onSubmit}
            disabled={submitting || !!result}
            style={{
              padding: "13px 21px", fontWeight: 600, borderRadius: "7px", border: "none", background: "#22c55e",
              color: "#fff", cursor: submitting || !!result ? "not-allowed" : "pointer",
              opacity: submitting || !!result ? 0.5 : 1, transition: "all .18s"
            }}
            aria-live="polite"
          >
            {submitting ? "Submitting..." : result ? "Submitted" : "Submit"}
          </button>
        )}
      </nav>

      <footer style={{
        marginTop: "2.2rem", textAlign: "center", fontSize: "0.92rem", color: "#64748b"
      }}>
        VaultCore &copy; 2026. For authorized use only.
      </footer>
    </main>
  );
}