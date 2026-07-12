# VaultCore Financial — Compliance Evidence

Maps each specification requirement (Zaalima Project 1: FinTech) to its implementation and test.

## Deep (production) features
| Requirement | Evidence | Test |
|---|---|---|
| Double-entry immutable ledger, ACID, serialized | `V1__create_ledger_schema.sql` (UPDATE/DELETE trigger, deferred debit=credit trigger, CHECK constraints); `LedgerService.validateDoubleEntry`; `TransferService` (SERIALIZABLE + locks) | `LedgerImmutabilityIT`, `LedgerDoubleEntryIT`, `TransferServiceConcurrencyIT` |
| Virtual Threads for "Get Balance" | `VirtualThreadConfig` (`newVirtualThreadPerTaskExecutor`) wired into `LedgerQueryService.getBalance`; `spring.threads.virtual.enabled=true` | Exercised by ledger/portfolio ITs |
| Fraud detection middleware (AOP) + mock 2FA | `FraudDetectionAspect` (AOP) + `FraudChallengeService` (issue/verify/consume, expiry) + `FraudNotificationService` (mock) | `FraudChallengeFlowIT` |

## Week-by-week review criteria
| Week | Criterion | Status | Evidence |
|---|---|---|---|
| 1 | Manually modifying a ledger row is prevented | ✅ | `LedgerImmutabilityIT` |
| 1 | Spring Security + JWT (+ refresh) | ✅ (JWT via Keycloak; refresh handled by Keycloak adapter) | `SecurityConfig`, `auth.js` |
| 2 | 100 concurrent withdrawals, correct & non-negative | ✅ | `TransferServiceConcurrencyIT` |
| 2 | Send-money multi-step wizard | ✅ | `frontend/src/pages/SendMoney.jsx` |
| 3 | Stock price round-trip under 300ms | ⚠️ mechanism present (250ms timeout + cache); end-to-end assertion outstanding | `StockPriceClient`, mock API test |
| 3 | Portfolio dashboard (Recharts) | ✅ | `frontend/src/pages/Portfolio.jsx` |
| 4 | Audit logging via AOP | ✅ (Spring AOP; console) — durable audit table outstanding | `AuditLoggingAspect`, `AuditAspectTest` |
| 4 | PDF monthly statements | ✅ multi-page, external lib (PDFBox) | `StatementPdfRenderer` |
| 4 | OWASP ZAP scan | ⚠️ outstanding — see `RUNBOOK.md` / CI Trivy scan | — |

## Hardening added beyond the base spec (audit remediation)
| Area | Evidence |
|---|---|
| Object-level authorization / IDOR prevention | `AccountOwnershipService`, `CurrentUserProvider`, `TransferAuthorizationIT` |
| Idempotent money movement | `IdempotencyService`, `V7__create_idempotency_keys.sql` |
| Fraud 2FA completion path | `FraudChallengeController`, `V8__create_fraud_challenges.sql` |
| BCrypt credential hashing | `MethodSecurityConfig`, `AdminProvisioningController` |
| CORS + security headers | `SecurityConfig` |
| Non-root containers + CI + image scan | `Dockerfile`, `docker/mock-stock-api/Dockerfile`, `.github/workflows/ci.yml` |

## Outstanding (tracked)
Persistent audit table + correlation IDs; rate limiting; Hibernate L2 cache entity opt-in; connection
pool sizing; OWASP ZAP report; frontend state-management library; full README API/schema sections.
