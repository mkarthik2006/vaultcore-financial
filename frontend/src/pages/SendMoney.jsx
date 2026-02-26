import React, { useMemo, useState } from "react";
import { createTransfer } from "../services/transferApi";

const STEPS = {
  RECIPIENT: 1,
  AMOUNT: 2,
  CONFIRM: 3,
};

export default function SendMoney() {
  const [step, setStep] = useState(STEPS.RECIPIENT);

  const [fromAccount, setFromAccount] = useState("A001"); // optional default for demo/testing
  const [toAccount, setToAccount] = useState("");
  const [amount, setAmount] = useState("");
  const [currency, setCurrency] = useState("USD");

  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null); // { transactionReferenceId, ledgerTransactionId }
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

  return (
    <div style={{ maxWidth: 520, margin: "0 auto", padding: 16 }}>
      <h2>Send Money</h2>

      {error && (
        <div style={{ background: "#fee", border: "1px solid #f99", padding: 10, marginBottom: 12 }}>
          <strong>Error:</strong> {error}
        </div>
      )}

      {result && (
        <div style={{ background: "#efe", border: "1px solid #9f9", padding: 10, marginBottom: 12 }}>
          <div><strong>Transfer submitted</strong></div>
          <div>Transaction Reference ID: <code>{result.transactionReferenceId}</code></div>
          <div>Ledger Transaction ID: <code>{result.ledgerTransactionId}</code></div>
        </div>
      )}

      <div style={{ marginBottom: 12 }}>
        <strong>Step {step} of 3</strong>
      </div>

      {step === STEPS.RECIPIENT && (
        <>
          <label style={{ display: "block", marginBottom: 6 }}>
            From Account
            <input
              value={fromAccount}
              onChange={(e) => setFromAccount(e.target.value)}
              style={{ display: "block", width: "100%", padding: 8, marginTop: 4 }}
              placeholder="A001"
            />
          </label>
          {validation.fromAccount && <div style={{ color: "crimson" }}>{validation.fromAccount}</div>}

          <label style={{ display: "block", marginBottom: 6, marginTop: 10 }}>
            Recipient Account
            <input
              value={toAccount}
              onChange={(e) => setToAccount(e.target.value)}
              style={{ display: "block", width: "100%", padding: 8, marginTop: 4 }}
              placeholder="A002"
            />
          </label>
          {validation.toAccount && <div style={{ color: "crimson" }}>{validation.toAccount}</div>}

          <label style={{ display: "block", marginBottom: 6, marginTop: 10 }}>
            Currency
            <input
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
              style={{ display: "block", width: "100%", padding: 8, marginTop: 4, textTransform: "uppercase" }}
              placeholder="USD"
            />
          </label>
          {validation.currency && <div style={{ color: "crimson" }}>{validation.currency}</div>}
        </>
      )}

      {step === STEPS.AMOUNT && (
        <>
          <div style={{ marginBottom: 10 }}>
            <div><strong>From:</strong> {fromAccount}</div>
            <div><strong>To:</strong> {toAccount}</div>
            <div><strong>Currency:</strong> {currency.toUpperCase()}</div>
          </div>

          <label style={{ display: "block", marginBottom: 6 }}>
            Amount
            <input
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              style={{ display: "block", width: "100%", padding: 8, marginTop: 4 }}
              placeholder="200.00"
              inputMode="decimal"
            />
          </label>
          {validation.amount && <div style={{ color: "crimson" }}>{validation.amount}</div>}
        </>
      )}

      {step === STEPS.CONFIRM && (
        <>
          <div style={{ marginBottom: 10 }}>
            <div><strong>From:</strong> {fromAccount}</div>
            <div><strong>To:</strong> {toAccount}</div>
            <div><strong>Amount:</strong> {amount}</div>
            <div><strong>Currency:</strong> {currency.toUpperCase()}</div>
          </div>

          {!result && (
            <div style={{ background: "#f7f7f7", padding: 10, border: "1px solid #ddd" }}>
              Confirm the transfer details, then click Submit.
            </div>
          )}
        </>
      )}

      <div style={{ display: "flex", gap: 8, marginTop: 16 }}>
        <button
          type="button"
          onClick={() => setStep((s) => Math.max(STEPS.RECIPIENT, s - 1))}
          disabled={submitting || step === STEPS.RECIPIENT}
        >
          Back
        </button>

        {step !== STEPS.CONFIRM && (
          <button
            type="button"
            onClick={() => canGoNext && setStep((s) => s + 1)}
            disabled={submitting || !canGoNext}
          >
            Next
          </button>
        )}

        {step === STEPS.CONFIRM && (
          <button type="button" onClick={onSubmit} disabled={submitting || !!result}>
            {submitting ? "Submitting..." : result ? "Submitted" : "Submit"}
          </button>
        )}
      </div>
    </div>
  );
}