import { apiFetch } from "./apiClient";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8082";

/**
 * Thrown when the backend's fraud-detection aspect (an @Before interceptor on
 * TransferService.transfer()) flags a transfer and blocks it pending 2FA. The 403 body carries
 * everything needed to complete the challenge and resubmit: { challengeId, channel, expiresAt }.
 */
export class FraudChallengeRequiredError extends Error {
  constructor(body) {
    super(body?.message || "Additional verification is required to complete this transfer.");
    this.name = "FraudChallengeRequiredError";
    this.challengeId = body?.challengeId;
    this.channel = body?.channel;
    this.expiresAt = body?.expiresAt;
  }
}

async function parseErrorBody(res) {
  try {
    return await res.json();
  } catch {
    return null;
  }
}

/**
 * payload: { fromAccount, toAccount, amount, currency }
 * challengeId: pass the verified challenge's id to resubmit a transfer that was previously
 * blocked for 2FA (sent as the X-Fraud-Challenge-Id header the backend expects on resubmission).
 */
export async function createTransfer(payload, { challengeId } = {}) {
  const res = await apiFetch(`${API_BASE_URL}/api/v1/transfers`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(challengeId ? { "X-Fraud-Challenge-Id": challengeId } : {}),
    },
    body: JSON.stringify(payload),
  });

  if (res.status === 403) {
    const body = await parseErrorBody(res);
    if (body?.error === "fraud_challenge_required") {
      throw new FraudChallengeRequiredError(body);
    }
    throw new Error(body?.message || "You're not authorized to transfer from this account.");
  }

  if (!res.ok) {
    const body = await parseErrorBody(res);
    throw new Error(body?.message || body?.error || `HTTP ${res.status}`);
  }

  return res.json();
}

/** POST /api/v1/fraud/challenges/{challengeId}/verify — completes the 2FA step. */
export async function verifyFraudChallenge(challengeId, code) {
  const res = await apiFetch(`${API_BASE_URL}/api/v1/fraud/challenges/${encodeURIComponent(challengeId)}/verify`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ code }),
  });

  if (!res.ok) {
    const body = await parseErrorBody(res);
    throw new Error(body?.message || `Verification failed (HTTP ${res.status})`);
  }

  return res.json();
}
