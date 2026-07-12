# VaultCore Financial — Release Notes

## v1.0.0 — Enterprise Edition
**Release date:** 2026-07-10  •  **Branch:** `feature/enterprise-hardening`  •  **Status:** Release candidate (pending push-triggered CI run)

---

## 1. Overview

v1.0.0 is the first enterprise-grade release of the VaultCore Financial banking core. It delivers the
three specification-mandated deep features (immutable double-entry ledger, virtual-thread balance reads,
fraud AOP + 2FA) and hardens the system to production standards: externalized OIDC auth, object-level
authorization, idempotent money movement, durable audit, rate limiting, and a one-command Docker
deployment — all runtime-verified (tests, Docker, OWASP ZAP, live real-token security probes).

---

## 2. Major Features

| Feature | Detail |
|---|---|
| **Double-entry immutable ledger** | DB trigger blocks UPDATE/DELETE; deferred trigger enforces Σdebit=Σcredit; balance derived from entries. |
| **Virtual-thread balance reads** | Java 21 `newVirtualThreadPerTaskExecutor` on the "Get Balance" path + global virtual threads. |
| **Concurrency-safe transfers** | `SERIALIZABLE` + pessimistic locks + deterministic ordering + serialization-retry (≤30). |
| **Fraud 2FA (AOP)** | Spring AOP aspect; persisted challenges (BCrypt code, TTL); verify endpoint; resubmit to complete. |
| **Idempotent transfers** | `Idempotency-Key`, reserve-before-execute (UNIQUE), replay, release-on-failure. |
| **Trading** | Portfolio + holdings, mock stock API via `RestClient` (250 ms timeout, 2 s cache), Recharts dashboard. |
| **PDF statements** | Multi-page monthly statements (PDFBox) with paginated ledger line items. |
| **React SPA** | Keycloak PKCE login, 3-step Send-Money wizard, portfolio dashboard, admin provisioning. |

---

## 3. Architecture Improvements

- Single nginx **gateway** as the sole public entry point (SPA + API + Keycloak under one origin).
- Health-gated Compose startup; the stack converges under `docker compose up`.
- Clean layering (controllers → services → repositories), DTO boundary, consolidated exception advice.
- **Zustand** introduced for frontend auth/session state; React error boundary.

## 4. Security Improvements

- **Broken access control fixed:** object-level ownership (IDOR prevention) on transfer + balance;
  RBAC via method security. Runtime-proven with a real Keycloak token (IDOR/RBAC → `403`).
- Method security enabled in **all** profiles (authz now tested).
- **BCrypt** for local credentials (removed plaintext placeholder).
- CORS policy + security headers (Spring on `/api`, nginx on SPA), `server_tokens off`.
- Per-IP **rate limiting**; **correlation IDs**; durable **audit_log**.
- **OWASP ZAP baseline: 0 High / 0 Critical** (one accepted Medium documented).
- Non-root containers; CI **Trivy** image scan + **CodeQL**.

## 5. Performance Improvements

- Virtual threads on reads; Hibernate **second-level cache** (Redis/Redisson) on `Account`;
  Spring cache on balances; JDBC batching + `default_batch_fetch_size`; composite ledger index (`V9`).

## 6. Testing Improvements

- **35** automated tests, **BUILD SUCCESS**; Testcontainers PostgreSQL 15.
- Added Mockito **unit tier** and MockMvc **security/IDOR/idempotency/fraud** tests.
- JaCoCo coverage: **73.9% instructions / 76.9% lines / 48.3% branches**.

## 7. CI/CD Improvements

- GitHub Actions: backend `mvn verify` + coverage, frontend lint/build, Trivy, CodeQL.

## 8. Notable Fixes (found by running the system)

| Fix | Impact |
|---|---|
| Keycloak PG15 `permission denied for schema public` | Stack would not start on a fresh deploy — now fixed (keycloak owns its schema). |
| Production security chain now active in tests | Fixed false 403s; closed "authz untested" gap. |
| Duplicate exception advice consolidated | Fraud 403 now returns `challengeId` as intended. |
| `FraudChallenge.amount` precision | Prevented Hibernate `validate` boot failure. |
| Gateway healthcheck `localhost`→`127.0.0.1` | Gateway now reports healthy. |
| Redis host port `6379`→`6380` | Avoids clashes with a local/other-project Redis. |

---

## 9. Known Limitations

1. **CI hosted run** — the workflow is authored and locally verified; the GitHub Actions run itself
   requires a push to observe (not yet pushed).
2. **CSP `style-src 'unsafe-inline'`** — accepted Medium (required by React inline styles).
3. **Rate limiting** is per-instance (in-memory); multi-instance needs Redis counters.
4. **Keycloak DB password** is a dev value — source from a secret manager for production.
5. **`refresh_tokens`** table is reserved/unused (refresh handled by Keycloak).
6. **Frontend fraud-challenge UI** — the backend flow is complete; a SPA prompt for the 2FA code is
   future work.
7. Active (authenticated) OWASP ZAP scan and a load/perf suite are future work.

---

## 10. Migration / Upgrade Notes

- **Fresh deploy:** `docker compose down -v && docker compose up -d --build` (drops volumes so the
  PG15-corrected Keycloak init runs). Provision at least one account with `ownerUsername` so transfers
  pass the ownership check.
- **Database:** Flyway V1–V10 applied automatically; append-only — never edit applied migrations.
- **Redis host port** moved to `6380` (internal `6379` unchanged) — update any external tooling.
- **Env:** add `APP_CORS_ALLOWED_ORIGINS`, `APP_RATE_LIMIT_*`, `FRAUD_CHALLENGE_TTL_SECONDS` as needed
  (sane defaults provided).

---

## 11. Future Roadmap

1. Push branch → observe green GitHub Actions; flip Trivy/CodeQL to enforcing after triage.
2. Redis-backed distributed rate limiting.
3. Materialized running-balance snapshots.
4. Frontend fraud-challenge UX.
5. Secret-manager integration for all credentials.
6. Authenticated active ZAP scan + load testing.

---

### Document Control

| Field | Value |
|---|---|
| ✔ Document Status | **Approved — release candidate** |
| ✔ Last Reviewed | 2026-07-10 |
| ✔ Version | 1.0.0 |
| ✔ Related Documents | [01_Enterprise_Architecture](01_Enterprise_Architecture.md), [05_Security_Assessment](05_Security_Assessment.md), [07_Test_Strategy_and_Coverage](07_Test_Strategy_and_Coverage.md), [08_Deployment_Guide](08_Deployment_Guide.md) |
