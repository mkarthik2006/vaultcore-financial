# VaultCore Financial — Compliance Evidence

Maps each specification requirement (Zaalima Project 1: FinTech) to its implementation and test.
Reflects the **current** codebase and runtime verification: `mvn clean verify` → 35/35 tests,
`docker compose up` → all services healthy, OWASP ZAP baseline 0 High/0 Critical, and live real-JWT
security probes (401/403). See also [05_Security_Assessment.md](05_Security_Assessment.md),
[06_OWASP_ZAP_Report.md](06_OWASP_ZAP_Report.md), [07_Test_Strategy_and_Coverage.md](07_Test_Strategy_and_Coverage.md).

## Deep (production) features
| Requirement | Evidence | Test |
|---|---|---|
| Double-entry immutable ledger, ACID, serialized | `V1__create_ledger_schema.sql` (UPDATE/DELETE trigger, deferred debit=credit trigger, CHECK constraints); `LedgerService`; `TransferService` (SERIALIZABLE) | `LedgerImmutabilityIT`, `LedgerDoubleEntryIT`, `TransferServiceConcurrencyIT` |
| Virtual Threads for "Get Balance" | `VirtualThreadConfig` (`newVirtualThreadPerTaskExecutor`) wired into `LedgerQueryService.getBalance` | ledger/portfolio ITs |
| Fraud detection middleware (AOP) + mock 2FA | `FraudDetectionAspect` + `FraudChallengeService` (issue/verify/consume, TTL) + `FraudNotificationService` (mock) | `FraudChallengeFlowIT`, `FraudDetectionServiceTest` |

## Audit logging — Spring AOP vs. compile-time AspectJ (clarification)
The spec asks for "Audit Logging (**using AspectJ**) to log every method call parameters and return
values." This is implemented with **Spring AOP**:
- `AuditLoggingAspect` is an `@Aspect` using **AspectJ pointcut-expression syntax**
  (`@Around("execution(* com.vaultcore..*Controller.*(..)) || execution(* com.vaultcore..*Service.*(..))")`),
  woven at **runtime via Spring's proxy-based AOP** (`spring-boot-starter-aop`) — **not** the AspectJ
  compiler (`ajc`) or a load-time-weaving agent.
- It logs **both parameters and the return value** (plus duration), e.g.
  `AUDIT method=... params=[...] result=... durationMs=...`, satisfying the functional requirement
  (verified by `AuditAspectTest`).

**Why proxy-based, not compile-time weaving:** it needs no extra build tooling
(`aspectj-maven-plugin`/weaver agent), keeps a standard Spring stack, and advises every Spring-managed
`*Controller`/`*Service` bean — sufficient for the audited entry points. **Documented limitation:**
self-invocation within a bean is not advised (a proxy-AOP property); acceptable here because audited
operations are invoked as Spring beans. Beyond the spec, audit events are also **persisted** to a
durable `audit_log` table (`V10`) with **correlation IDs** (`CorrelationIdFilter`).

## Week-by-week review criteria
| Week | Criterion | Status | Evidence |
|---|---|---|---|
| 1 | Manually modifying a ledger row is prevented | ✅ | `LedgerImmutabilityIT` |
| 1 | Spring Security + JWT (+ refresh) | ✅ (JWT via Keycloak; **refresh delegated to Keycloak**; unused `refresh_tokens` table dropped in `V11`) | `SecurityConfig`, `auth.js` |
| 2 | 100 concurrent withdrawals, correct & non-negative | ✅ | `TransferServiceConcurrencyIT` |
| 2 | Send-money multi-step wizard | ✅ | `SendMoney.jsx` (4 steps) |
| 3 | Stock price round-trip under 300ms | ✅ | 250ms client timeout + cache; `StockPriceClientTest.priceRoundTripIsUnder300ms` asserts `<300ms` |
| 3 | Portfolio dashboard (Recharts) | ✅ | `Portfolio.jsx` |
| 4 | Audit logging via AOP | ✅ (Spring AOP; params+return; durable table) | `AuditLoggingAspect`, `AuditAspectTest` |
| 4 | PDF monthly statements | ✅ multi-page (PDFBox) | `StatementPdfRenderer` |
| 4 | OWASP ZAP scan | ✅ 0 High/0 Critical | `security/zap/` |

## Enterprise hardening (all implemented & verified)
| Area | Evidence | Status |
|---|---|---|
| Object-level authorization / IDOR prevention | `AccountOwnershipService`, `CurrentUserProvider` | ✅ (runtime 403) |
| Function-level RBAC | `@PreAuthorize("hasRole('ADMIN')")` + URL rule | ✅ (runtime 403) |
| Idempotent money movement | `IdempotencyService`, `V7` (UNIQUE) | ✅ |
| Fraud 2FA completion path | `FraudChallengeController`, `V8` | ✅ |
| BCrypt credential hashing | `MethodSecurityConfig` | ✅ |
| CORS + security headers | `SecurityConfig` + nginx gateway | ✅ |
| **Rate limiting — Redis counters** | `RateLimitFilter` (`StringRedisTemplate` INCR/EXPIRE, fail-open, distributed) | ✅ (`RateLimitFilterTest`) |
| Durable audit trail + correlation IDs | `audit_log` (`V10`), `AuditEventService`, `CorrelationIdFilter` | ✅ |
| Hibernate L2 cache (Redis) | `@Cache` on `Account` + `RedissonRegionFactory` | ✅ |
| Frontend state management | **Zustand** `authStore` | ✅ |
| Non-root containers, CI, image scan, CodeQL | Dockerfiles, `.github/workflows/ci.yml`, Trivy, CodeQL | ✅ |

## Redis usage against the spec ("caching, session management, rate-limiting counters")
- **Caching:** ✅ Hibernate L2 (Redisson region factory) + Spring cache (`spring.cache.type: redis`).
- **Rate-limiting counters:** ✅ now Redis-backed (`RateLimitFilter`, INCR/EXPIRE per client/window).
- **Session management:** intentionally **not used** — the API is stateless (bearer JWT, no server
  sessions), which is the more scalable/secure design; there is no session to store in Redis.

## Genuinely outstanding (tracked, non-blocking)
- **HikariCP pool sizing** — not explicitly configured (Spring Boot defaults); tune before real load.
- Frontend fraud-challenge UX (the backend challenge→verify→resubmit flow is complete).
- Authenticated (active) OWASP ZAP scan + a dedicated load/perf suite.
- Keycloak DB credential → source from a secret manager for production.
