# VaultCore Financial — API Reference

> Base URL (via gateway): `http://localhost:8082`  •  All API paths are prefixed `/api/v1`.
> All endpoints require a valid Keycloak-issued **Bearer** JWT unless stated otherwise. Money-movement
> and balance endpoints additionally enforce **account ownership**. Every response includes an
> `X-Correlation-Id` header. This reference is derived directly from the controllers in
> `com.vaultcore.core.**`.

---

## Conventions

| Item | Value |
|---|---|
| Auth scheme | `Authorization: Bearer <access_token>` |
| Content type | `application/json` (except statement PDF) |
| Unauthenticated | `401 Unauthorized` (JSON via `RestAuthenticationEntryPoint`) |
| Forbidden (not owner / not admin) | `403` with `{"error":"access_denied", ...}` |
| Validation failure | `400` with `{"error":"validation_failed","details":{...}}` |
| Correlation | request/response header `X-Correlation-Id` (generated if absent) |

**Runtime-verified status codes** (real Keycloak JWT, live stack): unauthenticated balance → `401`;
IDOR balance/transfer → `403`; non-admin → admin endpoint → `403`; tampered token → `401`.

---

## 1. Transfers

### `POST /api/v1/transfers`

Create a money transfer (balanced debit/credit ledger transaction).

| | |
|---|---|
| **Auth** | Bearer JWT; caller must **own** `fromAccount` |
| **Headers** | `Authorization` (required); `Idempotency-Key` (optional); `X-Fraud-Challenge-Id` (optional, to satisfy a 2FA challenge) |

**Request body**

```json
{
  "fromAccount": "ACME-001",
  "toAccount": "ACME-002",
  "amount": 250.00,
  "currency": "USD"
}
```

**Validation:** `fromAccount`/`toAccount` `@NotBlank`; `amount` `@NotNull @Positive`;
`currency` 3 letters `^[A-Za-z]{3}$`.

**Responses**

| Status | When | Body |
|---|---|---|
| `201 Created` | success (also on idempotent replay) | `{ "transactionReferenceId": "...", "ledgerTransactionId": "..." }` + `Location` header |
| `400` | validation / bad request | `validation_failed` / `invalid_request` |
| `401` | no/invalid token | — |
| `403` | not owner of `fromAccount` | `access_denied` |
| `403` | amount ≥ threshold, no verified challenge | `fraud_challenge_required` (see below) |
| `409` | insufficient funds | `ApiError` (409) |
| `409` | idempotency key reused with different payload / in progress | `idempotency_conflict` |

**Fraud-challenge response (403)**

```json
{
  "error": "fraud_challenge_required",
  "message": "Transfer exceeds fraud threshold; 2FA challenge required.",
  "channel": "mock-sms",
  "challengeId": "3f2a...",
  "expiresAt": "2026-07-10T12:00:00Z",
  "verifyUrl": "/api/v1/fraud/challenges/3f2a.../verify",
  "resubmitHeader": "X-Fraud-Challenge-Id"
}
```

**Example**

```bash
curl -X POST http://localhost:8082/api/v1/transfers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Idempotency-Key: 6b1e-..." \
  -H "Content-Type: application/json" \
  -d '{"fromAccount":"ACME-001","toAccount":"ACME-002","amount":250.00,"currency":"USD"}'
```

---

## 2. Ledger / Balance

### `GET /api/v1/ledger/balance`

Return the computed balance for an owned account. Executed on a **virtual thread**.

| Param | In | Required | Notes |
|---|---|---|---|
| `accountNumber` | query | yes | must be owned by the caller |
| `currency` | query | yes | 3-letter code |

| Status | When | Body |
|---|---|---|
| `200 OK` | owner | numeric balance, e.g. `4800.00` |
| `401` | unauthenticated | — |
| `403` | not owner / unknown account | `access_denied` |

```bash
curl -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8082/api/v1/ledger/balance?accountNumber=ACME-001&currency=USD"
```

---

## 3. Fraud Challenges

### `POST /api/v1/fraud/challenges/{challengeId}/verify`

Verify a 2FA challenge code delivered (mock) after a large transfer attempt.

| | |
|---|---|
| **Auth** | Bearer JWT |
| **Path** | `challengeId` (UUID) |

**Request**

```json
{ "code": "123456" }
```

`code` must match `^[0-9]{6}$`.

| Status | When | Body |
|---|---|---|
| `200 OK` | verified | `{ "challengeId": "...", "status": "VERIFIED", "message": "..." }` |
| `400` | unknown / expired / already used / wrong code | `invalid_request` |

After a `200`, resubmit the original transfer with `X-Fraud-Challenge-Id: {challengeId}`.

---

## 4. Portfolio (Trading)

Portfolio data is scoped to the authenticated user (`preferred_username`).

### `GET /api/v1/portfolio`
Return the caller's portfolio (holdings + market values).

### `POST /api/v1/portfolio/holdings`
Add a holding.

```json
{ "symbol": "AAPL", "quantity": 10, "price": 185.32 }
```

### `GET /api/v1/portfolio/valuation`
Return the portfolio valued at current (mock) market prices.

| Status | When |
|---|---|
| `200 OK` | success |
| `400` | user not provisioned locally / invalid input |
| `401` | unauthenticated |

**Response shape (`PortfolioDTO`)** — user, list of `HoldingDTO` (symbol, quantity, avgPrice,
marketPrice, marketValue), totals. Prices come from `StockPriceClient` (250 ms timeout, 2 s cache).

---

## 5. Statements

### `GET /api/v1/statements/monthly`

Generate and download a **PDF** monthly statement (multi-page).

| Param | In | Required |
|---|---|---|
| `accountNumber` | query | yes |
| `month` | query | yes — `YYYY-MM` (parsed as `YearMonth`) |

| Status | Response |
|---|---|
| `200 OK` | `application/pdf`, `Content-Disposition: attachment; filename=statement-<acct>-<month>.pdf` |
| `401` | unauthenticated |

```bash
curl -H "Authorization: Bearer $TOKEN" -OJ \
  "http://localhost:8082/api/v1/statements/monthly?accountNumber=ACME-001&month=2026-07"
```

The PDF includes account, currency, month, opening/closing balances, totals, and a paginated list of
ledger line items (dated, typed, amount, description).

---

## 6. Admin Provisioning  *(ROLE_ADMIN)*

Guarded by `@PreAuthorize("hasRole('ADMIN')")` **and** the URL rule `/api/v1/admin/** → hasRole('ADMIN')`.

### `POST /api/v1/admin/users`

```json
{ "email": "jane@bank.test", "username": "jane", "passwordHash": "optional-raw", "roles": "USER", "enabled": true }
```

- `email` `@NotBlank @Email`; `username` `@NotBlank`. Any provided password is **BCrypt-hashed**;
  when absent, a BCrypt hash of a random value is stored (never a usable/known credential).

| Status | Response |
|---|---|
| `201 Created` | `UserResponse` + `Location` |
| `400` | duplicate username/email or validation |
| `403` | caller lacks `ROLE_ADMIN` |

### `POST /api/v1/admin/accounts`

```json
{ "accountNumber": "ACME-001", "currency": "USD", "ownerUsername": "jane" }
```

- `ownerUsername` (optional) binds the account to a user so ownership can be enforced on transfers.

| Status | Response |
|---|---|
| `201 Created` | `AccountResponse` (id, accountNumber, currency) |
| `400` | duplicate account / bad currency / unknown owner |
| `403` | caller lacks `ROLE_ADMIN` |

---

## 7. Operational Endpoints

| Endpoint | Auth | Purpose |
|---|---|---|
| `GET /actuator/health` | permitAll | Liveness/readiness (used by Docker healthcheck) |
| `GET /actuator/info` | permitAll | Build info |
| `GET /health.json` (gateway) | none | Gateway health (`{"status":"UP"}`) |

---

## 8. Error Object Reference

```json
// Generic cross-cutting error
{ "error": "access_denied", "message": "You do not have permission to perform this action." }

// Validation
{ "error": "validation_failed", "details": { "amount": "must be greater than 0" } }

// Transfer-domain (ApiError)
{ "status": 409, "message": "Insufficient funds ...", "timestamp": "2026-07-10T12:00:00Z" }
```

---

### Document Control

| Field | Value |
|---|---|
| ✔ Document Status | **Approved — reflects current controllers** |
| ✔ Last Reviewed | 2026-07-10 |
| ✔ Version | 1.0.0 |
| ✔ Related Documents | [02_Software_Design_Document](02_Software_Design_Document.md), [04_Database_Design](04_Database_Design.md), [05_Security_Assessment](05_Security_Assessment.md) |
