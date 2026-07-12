import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { createTransfer, verifyFraudChallenge, FraudChallengeRequiredError } from "../services/transferApi";
import Card from "../components/ui/Card";
import PageHeader from "../components/ui/PageHeader";
import Alert from "../components/ui/Alert";
import Button from "../components/ui/Button";
import StepIndicator from "../components/ui/StepIndicator";

const STEPS = { RECIPIENT: 0, AMOUNT: 1, REVIEW: 2, SUCCESS: 3 };
const STEP_LABELS = ["Recipient", "Amount", "Review", "Done"];

function Field({ id, label, error, children }) {
  return (
    <div className="mb-3">
      <label htmlFor={id} className="form-label small vc-text-muted mb-1">{label}</label>
      {children}
      {error && <div className="small mt-1" style={{ color: "var(--vc-danger)" }}>{error}</div>}
    </div>
  );
}

export default function SendMoney() {
  const [step, setStep] = useState(STEPS.RECIPIENT);

  const [fromAccount, setFromAccount] = useState("");
  const [toAccount, setToAccount] = useState("");
  const [amount, setAmount] = useState("");
  const [currency, setCurrency] = useState("USD");

  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  // Fraud/2FA challenge state — populated when the backend's fraud-detection interceptor blocks
  // a transfer (403 fraud_challenge_required) and cleared once the challenge is verified.
  const [challenge, setChallenge] = useState(null);
  const [verifyCode, setVerifyCode] = useState("");
  const [verifying, setVerifying] = useState(false);
  const [verifyError, setVerifyError] = useState(null);

  const validation = useMemo(() => {
    const errors = {};
    if (!fromAccount.trim()) errors.fromAccount = "From account is required";
    if (!toAccount.trim()) errors.toAccount = "Recipient account is required";
    if (fromAccount.trim() && toAccount.trim() && fromAccount.trim() === toAccount.trim()) {
      errors.toAccount = "Recipient must be different from sender";
    }
    const parsed = Number(amount);
    if (step >= STEPS.AMOUNT) {
      if (!amount.trim()) errors.amount = "Amount is required";
      else if (Number.isNaN(parsed)) errors.amount = "Amount must be a number";
      else if (parsed <= 0) errors.amount = "Amount must be greater than 0";
    }
    if (currency.trim().length !== 3) errors.currency = "Currency must be 3 letters (e.g., USD)";
    return errors;
  }, [fromAccount, toAccount, amount, currency, step]);

  const canGoNext =
    (step === STEPS.RECIPIENT && !validation.fromAccount && !validation.toAccount && !validation.currency) ||
    (step === STEPS.AMOUNT && !validation.amount && !validation.currency);

  function buildPayload() {
    return {
      fromAccount: fromAccount.trim(),
      toAccount: toAccount.trim(),
      amount: Number(amount),
      currency: currency.trim().toUpperCase(),
    };
  }

  async function onSubmit() {
    setSubmitting(true);
    setError(null);
    try {
      const res = await createTransfer(buildPayload());
      setResult(res);
      setStep(STEPS.SUCCESS);
    } catch (e) {
      if (e instanceof FraudChallengeRequiredError) {
        setChallenge({ challengeId: e.challengeId, channel: e.channel, expiresAt: e.expiresAt, message: e.message });
      } else {
        setError(e?.message || "Transfer failed");
      }
    } finally {
      setSubmitting(false);
    }
  }

  async function onVerify(e) {
    e.preventDefault();
    setVerifying(true);
    setVerifyError(null);
    try {
      await verifyFraudChallenge(challenge.challengeId, verifyCode.trim());
      const res = await createTransfer(buildPayload(), { challengeId: challenge.challengeId });
      setResult(res);
      setChallenge(null);
      setStep(STEPS.SUCCESS);
    } catch (e) {
      setVerifyError(e?.message || "Verification failed");
    } finally {
      setVerifying(false);
    }
  }

  function startOver() {
    setStep(STEPS.RECIPIENT);
    setFromAccount("");
    setToAccount("");
    setAmount("");
    setCurrency("USD");
    setResult(null);
    setError(null);
    setChallenge(null);
    setVerifyCode("");
    setVerifyError(null);
  }

  return (
    <div className="vc-fade-in">
      <PageHeader
        breadcrumbs={[{ label: "Dashboard", to: "/dashboard" }, { label: "Send Money" }]}
        title="Send Money"
        description="Transfer funds between accounts with built-in fraud verification."
      />

      <Card style={{ maxWidth: 560, marginInline: "auto" }}>
        <StepIndicator steps={STEP_LABELS} activeIndex={step} />

        {error && <Alert tone="danger" className="mb-3">{error}</Alert>}

        {challenge && (
          <Alert tone="warning" title="Additional verification required" className="mb-3">
            {challenge.message} {challenge.channel && <>A code was sent via <strong>{challenge.channel}</strong>.</>}
          </Alert>
        )}

        {challenge ? (
          <form onSubmit={onVerify}>
            <Field id="verify-code" label="Verification code" error={verifyError}>
              <input
                id="verify-code"
                className="form-control"
                inputMode="numeric"
                autoComplete="one-time-code"
                placeholder="123456"
                value={verifyCode}
                onChange={(e) => setVerifyCode(e.target.value)}
                required
                autoFocus
              />
            </Field>
            <div className="d-flex gap-2 justify-content-center mt-4">
              <Button type="button" variant="outline" onClick={() => setChallenge(null)} disabled={verifying}>
                Back
              </Button>
              <Button type="submit" busy={verifying} busyLabel="Verifying…" icon="bi-shield-check">
                Verify &amp; Complete Transfer
              </Button>
            </div>
          </form>
        ) : (
          <>
            {step === STEPS.RECIPIENT && (
              <>
                <Field id="from-account" label="From account" error={validation.fromAccount}>
                  <input id="from-account" className="form-control" placeholder="ACC-0001" value={fromAccount} onChange={(e) => setFromAccount(e.target.value)} />
                </Field>
                <Field id="to-account" label="Recipient account" error={validation.toAccount}>
                  <input id="to-account" className="form-control" placeholder="ACC-0002" value={toAccount} onChange={(e) => setToAccount(e.target.value)} />
                </Field>
                <Field id="currency" label="Currency" error={validation.currency}>
                  <input id="currency" className="form-control text-uppercase" maxLength={3} placeholder="USD" value={currency} onChange={(e) => setCurrency(e.target.value)} />
                </Field>
              </>
            )}

            {step === STEPS.AMOUNT && (
              <>
                <div className="vc-surface-muted rounded-3 p-3 mb-3 small">
                  <div><strong>From:</strong> {fromAccount}</div>
                  <div><strong>To:</strong> {toAccount}</div>
                  <div><strong>Currency:</strong> {currency.toUpperCase()}</div>
                </div>
                <Field id="amount" label="Amount" error={validation.amount}>
                  <input id="amount" className="form-control" inputMode="decimal" placeholder="200.00" value={amount} onChange={(e) => setAmount(e.target.value)} autoFocus />
                </Field>
              </>
            )}

            {step === STEPS.REVIEW && (
              <>
                <div className="vc-surface-muted rounded-3 p-3 mb-3 small">
                  <div className="d-flex justify-content-between py-1"><span className="vc-text-muted">From</span><span className="fw-medium">{fromAccount}</span></div>
                  <div className="d-flex justify-content-between py-1"><span className="vc-text-muted">To</span><span className="fw-medium">{toAccount}</span></div>
                  <div className="d-flex justify-content-between py-1"><span className="vc-text-muted">Amount</span><span className="fw-medium">{amount} {currency.toUpperCase()}</span></div>
                </div>
                <p className="small vc-text-muted text-center mb-0">
                  Review the details above. Large or unusual transfers may require an additional verification step.
                </p>
              </>
            )}

            {step === STEPS.SUCCESS && result && (
              <div className="text-center py-3">
                <div
                  className="d-inline-flex align-items-center justify-content-center rounded-circle mb-3"
                  style={{ width: 56, height: 56, background: "var(--vc-emerald-100)", color: "var(--vc-emerald-700)" }}
                >
                  <i className="bi bi-check-lg fs-3" aria-hidden="true" />
                </div>
                <h2 className="h5 mb-1">Transfer submitted</h2>
                <p className="small vc-text-muted mb-3">Your funds are on their way.</p>
                <div className="vc-surface-muted rounded-3 p-3 text-start small mb-3 vc-mono">
                  <div>Transaction Ref: {result.transactionReferenceId}</div>
                  <div>Ledger Txn ID: {result.ledgerTransactionId}</div>
                </div>
                <div className="d-flex gap-2 justify-content-center">
                  <Link to="/dashboard" className="btn btn-outline-secondary">Back to dashboard</Link>
                  <Button icon="bi-arrow-repeat" onClick={startOver}>Send another</Button>
                </div>
              </div>
            )}

            {step !== STEPS.SUCCESS && (
              <div className="d-flex gap-2 justify-content-center mt-4">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => setStep((s) => Math.max(STEPS.RECIPIENT, s - 1))}
                  disabled={submitting || step === STEPS.RECIPIENT}
                >
                  Back
                </Button>
                {step !== STEPS.REVIEW && (
                  <Button type="button" onClick={() => canGoNext && setStep((s) => s + 1)} disabled={!canGoNext}>
                    Next
                  </Button>
                )}
                {step === STEPS.REVIEW && (
                  <Button type="button" onClick={onSubmit} busy={submitting} busyLabel="Submitting…" icon="bi-send-fill">
                    Submit Transfer
                  </Button>
                )}
              </div>
            )}
          </>
        )}
      </Card>
    </div>
  );
}
