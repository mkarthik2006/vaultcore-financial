# VaultCore Financial — Software Design Document (SDD)

> Reflects the current source under `src/main/java/com/vaultcore/**` and `frontend/src/**`.
> Cross-reference: [01_Enterprise_Architecture.md](01_Enterprise_Architecture.md).

---

## 1. Introduction

This SDD describes the internal design of the VaultCore Financial backend and frontend: how the code
is structured, which patterns are applied, and how the mandated features (immutable ledger, virtual
threads, fraud AOP) are realized. It is intended to let an engineer navigate and extend the codebase
without reverse-engineering it.

---

## 2. Requirements Traceability

| # | Requirement (spec) | Design element |
|---|---|---|
| R1 | Double-entry immutable ledger, ACID, serialized | `LedgerService`, `LedgerEntry` (`@Immutable`), DB triggers (V1), `TransferService` (`SERIALIZABLE`) |
| R2 | Virtual threads on "Get Balance" | `VirtualThreadConfig`, `LedgerQueryService.getBalance` |
| R3 | Fraud middleware + 2FA | `FraudDetectionAspect`, `FraudDetectionService`, `FraudChallengeService`, `FraudChallengeController` |
| R4 | Spring Security + JWT (+ refresh via IdP) | `SecurityConfig`, Keycloak, `keycloak-js` |
| R5 | React login page, Send-Money wizard, Portfolio dashboard | `LoginPage.jsx`, `SendMoney.jsx`, `Portfolio.jsx` (Recharts) |
| R6 | Mock stock API + REST client | `docker/mock-stock-api`, `StockPriceClient` |
| R7 | Audit logging (AOP) | `AuditLoggingAspect`, `AuditEventService` + `audit_log` |
| R8 | PDF monthly statements | `StatementService`, `StatementPdfRenderer` (PDFBox, multi-page) |
| R9 | Dockerized, one-command deploy | `docker-compose.yml`, `Dockerfile`, gateway |
| R10 | Concurrency correctness (100 threads) | pessimistic lock + `SERIALIZABLE` + `RetryableTransferExecutor` |

---

## 3. Design Principles

- **Clean Architecture / layering:** Controllers (web) → Services (domain) → Repositories
  (persistence). Controllers are thin; domain rules live in services; persistence is Spring Data.
- **DTO boundary:** Controllers accept/return records (DTOs), never JPA entities.
- **Fail closed:** Authorization denies by default; ownership failures return a generic 403.
- **Immutability where it matters:** Ledger entries are immutable at both the JPA (`@Immutable`,
  `updatable=false`) and database (trigger) layers.
- **Idempotency & retries as first-class concerns**, not afterthoughts.

### SOLID application

| Principle | Example |
|---|---|
| **S**RP | `TransferService` orchestrates; `RetryableTransferExecutor` owns retry policy; `LedgerService` owns ledger invariants; `FraudChallengeService` owns challenge lifecycle. |
| **O**CP | New fraud channels can be added behind `FraudNotificationService` without touching the aspect. |
| **L**SP | Repositories are Spring Data interfaces; substitutable in tests. |
| **I**SP | Small focused interfaces/records (`AddHoldingRequest`, `TransferRequestDTO`). |
| **D**IP | Controllers depend on service abstractions injected via the constructor; `@Lazy self` proxy for AOP-aware self-invocation. |

---

## 4. Package Structure (backend)

```
com.vaultcore
├── VaultcoreFinancialApplication        (@SpringBootApplication, @EnableCaching)
├── config
│   ├── VirtualThreadConfig              (newVirtualThreadPerTaskExecutor bean)
│   ├── MethodSecurityConfig             (@EnableMethodSecurity, PasswordEncoder [always-on])
│   ├── TransactionTemplateConfig
│   ├── CorrelationIdFilter              (MDC + X-Correlation-Id)
│   └── RateLimitFilter                  (per-IP fixed window)
├── security
│   ├── SecurityConfig                   (filter chain, CORS, headers, JWT decoder [!test])
│   ├── RestAuthenticationEntryPoint     (401 JSON)
│   └── CurrentUserProvider              (JWT → local UserEntity)
├── audit
│   ├── AuditLoggingAspect               (@Around *Controller/*Service)
│   ├── AuditEntry / AuditEventRepository / AuditEventService
├── exception
│   └── GlobalExceptionHandler           (@RestControllerAdvice)
├── user
│   ├── UserEntity / UserRepository
└── core
    ├── account   (Account, AccountRepository, AccountOwnershipService)
    ├── ledger    (LedgerEntry, LedgerService, LedgerQueryService, LedgerController, LedgerRepository)
    ├── transfer  (TransferService, TransferController, RetryableTransferExecutor,
    │              TransferRetryPolicy, Idempotency*, DTOs, TransferExceptionHandler)
    ├── transaction (TransactionReference[Repository])
    ├── fraud     (FraudDetectionAspect, FraudDetectionService, FraudChallengeService,
    │              FraudChallenge[Repository], FraudChallengeController, FraudNotificationService)
    ├── trading   (Portfolio, Holding, PortfolioService, PortfolioController, StockPriceClient, DTOs)
    ├── statement (StatementService, StatementController, StatementPdfRenderer, MonthlyStatement)
    └── admin     (AdminProvisioningController, Create*/*, *Response)
```

---

## 5. Frontend Modules

```
frontend/src
├── App.jsx                 (routing + guards driven by Zustand auth store)
├── main.jsx
├── store/authStore.js      (Zustand: authenticated, roles, username, selectors)
├── services
│   ├── auth.js             (keycloak-js adapter; syncs the Zustand store)
│   ├── apiClient.js        (fetch wrapper; Bearer token; 401 refresh-and-retry)
│   ├── transferApi.js / portfolioApi.js / adminApi.js
├── components
│   ├── LoginPage.jsx       (Login with Keycloak)
│   ├── Dashboard.jsx       (admin link gated on ADMIN role)
│   └── ErrorBoundary.jsx   (graceful render-error fallback)
└── pages
    ├── SendMoney.jsx       (3-step wizard: Recipient → Amount → Confirm)
    ├── Portfolio.jsx       (Recharts BarChart)
    └── AdminProvisioning.jsx
```

---

## 6. Design Patterns in Use

| Pattern | Location | Purpose |
|---|---|---|
| **DTO / Value Object** | `TransferRequestDTO`, `TransferResponseDTO`, `PortfolioDTO`, `HoldingDTO`, `AddHoldingRequest`, admin `Create*`/`*Response` (Java records) | Decouple API from entities; enable validation. |
| **Repository** | all `*Repository` interfaces (Spring Data JPA) | Persistence abstraction. |
| **Aspect (AOP)** | `FraudDetectionAspect`, `AuditLoggingAspect` | Cross-cutting fraud + audit without polluting domain code. |
| **Strategy** | `TransferRetryPolicy` (attempts/backoff/jitter) consumed by `RetryableTransferExecutor` | Pluggable retry behavior. |
| **Template method** | `IntegrationTestBase` (test), Spring `TransactionTemplate` config | Shared setup. |
| **Facade / Orchestrator** | `TransferService` | Coordinates ownership-agnostic idempotency + fraud + ledger. |
| **Proxy (self-invocation)** | `@Lazy TransferService self` | Ensures AOP/transactional advice applies to internal calls. |
| **Filter chain** | `CorrelationIdFilter`, `RateLimitFilter` | Ordered pre-processing. |

---

## 7. Key Sequence — Concurrency-Safe Transfer

```mermaid
sequenceDiagram
    autonumber
    participant C as Controller
    participant TS as TransferService
    participant RTE as RetryableTransferExecutor
    participant DB as PostgreSQL
    C->>TS: transfer(req, idemKey)
    TS->>DB: reserve idempotency (UNIQUE)
    Note over TS: self.transfer(req) via proxy → FraudDetectionAspect @Before fires
    TS->>RTE: executeWithRetry(lambda)
    loop until success or maxAttempts(30)
        RTE->>TS: self.transferInSerializableTx(req)
        Note over TS,DB: @Transactional(SERIALIZABLE, REQUIRES_NEW)
        TS->>DB: lock fromAcct/toAcct (ordered, PESSIMISTIC_WRITE)
        TS->>DB: check funds; insert DEBIT+CREDIT
        alt SQLSTATE 40001 (serialization)
            DB-->>RTE: CannotAcquireLock/40001 → retry (backoff+jitter)
        else success
            DB-->>TS: committed
        end
    end
    TS->>DB: idempotency complete + audit_log
    TS-->>C: TransferResponseDTO
```

---

## 8. Exception Handling Design

A single authoritative advice, `GlobalExceptionHandler` (`@RestControllerAdvice`), owns cross-cutting
mappings; `TransferExceptionHandler` owns only transfer-domain exceptions (no overlap — the duplicate
fraud/`IllegalArgumentException` handlers were consolidated).

| Exception | HTTP | Body `error` |
|---|---|---|
| `AccessDeniedException` | 403 | `access_denied` (generic; no resource disclosure) |
| `MethodArgumentNotValidException` | 400 | `validation_failed` (+ field details) |
| `IllegalArgumentException` | 400 | `invalid_request` |
| `IdempotencyConflictException` | 409 | `idempotency_conflict` |
| `FraudChallengeRequiredException` | 403 | `fraud_challenge_required` (+ `challengeId`, `expiresAt`, `verifyUrl`, `resubmitHeader`) |
| `AccountNotFoundException` | 404 | `ApiError` |
| `CurrencyMismatchException` | 400 | `ApiError` |
| `InsufficientFundsException` | 409 | `ApiError` |

No stack traces or internal identifiers are leaked in responses.

---

## 9. Validation Design

Bean Validation on request records, enforced with `@Valid` at controllers:

- `TransferRequestDTO`: `@NotBlank` accounts, `@NotNull @Positive` amount, `@Pattern("^[A-Za-z]{3}$")`
  currency.
- `AddHoldingRequest`, `CreateUserRequest` (`@Email`), `CreateAccountRequest` (`@Size(3,3)`),
  `VerifyChallengeRequest` (`@Pattern("^[0-9]{6}$")`).
- `TransferService.validateRequest` adds defense-in-depth server-side validation.

---

## 10. Configuration Design

`application.yaml` is environment-driven (`${VAR:default}`). Notable properties:

| Property | Purpose |
|---|---|
| `spring.threads.virtual.enabled: true` | Virtual threads for the servlet container |
| `spring.jpa.properties.hibernate.cache.*` | Redisson L2 region factory |
| `spring.jpa.properties.hibernate.jdbc.batch_size` / `default_batch_fetch_size` | batching / N+1 mitigation |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | Keycloak issuer |
| `app.fraud.threshold / challenge-enabled / challenge-channel / challenge-ttl-seconds` | fraud policy |
| `app.stock.api.base-url / timeout-ms / cache-ttl-ms` | stock client (250 ms timeout, 2 s cache) |
| `app.cors.allowed-origins` | CORS |
| `app.rate-limit.enabled / requests-per-minute` | rate limiting |
| `logging.pattern.level` | injects `correlationId` into logs |

The `test` profile (`application-test.yml` + `IntegrationTestBase`) disables L2 cache, Redis, and rate
limiting so tests need no external infrastructure beyond Testcontainers PostgreSQL.

---

## 11. Frontend Design Notes

- **State:** `authStore` (Zustand) holds derived auth state (`authenticated`, `roles`, `username`);
  `auth.js` pushes Keycloak state into it. Route guards and the admin link subscribe via selectors.
- **API client:** `apiClient.apiFetch` attaches the Bearer token and transparently refreshes on 401.
- **Wizard:** `SendMoney.jsx` is a 3-step state machine (`RECIPIENT → AMOUNT → CONFIRM`) with per-step
  validation gating "Next".
- **Resilience:** `ErrorBoundary` wraps the router.

---

### Document Control

| Field | Value |
|---|---|
| ✔ Document Status | **Approved — reflects current implementation** |
| ✔ Last Reviewed | 2026-07-10 |
| ✔ Version | 1.0.0 |
| ✔ Related Documents | [01_Enterprise_Architecture](01_Enterprise_Architecture.md), [03_API_Reference](03_API_Reference.md), [07_Test_Strategy_and_Coverage](07_Test_Strategy_and_Coverage.md) |
