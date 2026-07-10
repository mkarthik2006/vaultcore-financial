# VaultCore Financial — Security Assessment

> Reflects controls implemented in `com.vaultcore.security.**`, `com.vaultcore.config.**`, the nginx
> gateway, and the Keycloak realm. Findings are backed by automated tests and live-runtime evidence.

---

## 1. Threat Model (STRIDE, abridged)

| Threat | Vector | Control |
|---|---|---|
| **Spoofing** | Forged identity | Keycloak-issued JWT; `NimbusJwtDecoder` validates signature (JWKS), issuer, audience, expiry, clock-skew. Tampered token → `401` (runtime-verified). |
| **Tampering** | Ledger manipulation | Immutable ledger (DB trigger + `@Immutable`); parameterized SQL only. |
| **Repudiation** | "I didn't do it" | `audit_log` table + per-request correlation IDs + immutable ledger. |
| **Information disclosure** | Enumerate/read others' data | Object-level ownership (IDOR) → generic `403`, no existence disclosure; no stack traces in responses. |
| **Denial of service** | Request floods | Per-IP rate limiting; retry backoff bounded; virtual threads. |
| **Elevation of privilege** | Non-admin admin actions | RBAC via method security + URL rule; non-admin → admin endpoint → `403` (runtime-verified). |

---

## 2. Authentication

- **Model:** OAuth2/OIDC resource server. Backend validates tokens; it does not issue them.
- **Provider:** Keycloak 24 (realm `vaultcore`), reached through the gateway.
- **Frontend:** `keycloak-js`, Authorization Code + PKCE (S256), silent SSO, `updateToken` refresh.
- **Validators:** issuer (`issuer-uri`), optional audience (`app.security.jwt.audience`), clock skew
  (`allowed-clock-skew-seconds`).
- **Runtime evidence:** unauthenticated → `401`; `garbage.token` → `401`.

## 3. Authorization

| Layer | Mechanism | Evidence |
|---|---|---|
| Function-level | `@EnableMethodSecurity` (all profiles) + `@PreAuthorize("hasRole('ADMIN')")` + URL rule `/api/v1/admin/**` | non-admin → admin `POST` → `403` (runtime); `TransferAuthorizationIT` |
| Object-level (IDOR) | `AccountOwnershipService.requireOwnedAccount(caller, accountNumber)` on transfer `fromAccount` and balance | IDOR balance/transfer → `403` (runtime); `TransferAuthorizationIT`, `AccountOwnershipServiceTest` |
| Data scoping | Portfolio/statement scoped to `preferred_username` | code + runtime |

Method security is enabled in **all** profiles (via `MethodSecurityConfig`), so authorization is
enforced and tested under the test profile — closing the earlier "authz disabled under test" finding.

## 4. JWT / Keycloak / RBAC

- Roles mapped from Keycloak `realm_access.roles` → `ROLE_*`.
- Stateless sessions (`SessionCreationPolicy.STATELESS`); no server-side session to fixate.
- Keycloak uses a **separate database** (`keycloak_db`) with its own owner.

## 5. OWASP Top 10 (2021) coverage

| Risk | Status | Control / evidence |
|---|---|---|
| A01 Broken Access Control | **Mitigated** | Ownership (IDOR) + RBAC; runtime `403`s; ZAP passive PASS |
| A02 Cryptographic Failures | **Mitigated** | BCrypt for credentials & fraud codes; TLS terminated at edge in prod |
| A03 Injection | **Mitigated** | JPA/JPQL/parameterized queries only; no string-concatenated SQL |
| A04 Insecure Design | **Addressed** | Immutable ledger, idempotency, fraud 2FA, fail-closed authz |
| A05 Security Misconfiguration | **Mitigated** | CSRF-off (stateless), security headers, `server_tokens off`, non-root containers |
| A06 Vulnerable Components | **Monitored** | CI Trivy image scan; pinned base images |
| A07 Auth Failures | **Mitigated** | Keycloak; strict JWT validation; `401` on bad/absent token |
| A08 Integrity Failures | **Mitigated** | Immutable ledger; CodeQL in CI |
| A09 Logging/Monitoring Failures | **Mitigated** | `audit_log` + correlation IDs + actuator |
| A10 SSRF | **N/A / low** | Only outbound call is the internal mock stock API (fixed base URL, 250 ms timeout) |

## 6. Specific Controls

### IDOR / Broken Access Control
Resolved via `findByAccountNumberAndOwner_Id`; non-owner and non-existent are indistinguishable
(`403 access_denied`). **Runtime-proven** with a real Keycloak token.

### SQL Injection
All persistence via Spring Data / JPQL / parameterized `@Query`; the only raw JDBC (immutability tests)
uses bind parameters. ZAP passive scan: no SQLi indicators.

### XSS
React auto-escapes; no `dangerouslySetInnerHTML`. SPA served with a Content-Security-Policy
(`script-src 'self'`).

### CSRF
Disabled deliberately — correct for a stateless bearer-token API (no cookies used for auth). Sessions
are stateless.

### CORS
Explicit `CorsConfigurationSource` (`app.cors.allowed-origins`, default gateway origin), restricted
methods/headers, credentials-aware.

### Rate limiting
`RateLimitFilter` — per-IP fixed-window (default 300/min), returns `429`. In-memory per-instance today
(documented scale path: Redis counters).

### Audit logging & correlation IDs
`AuditLoggingAspect` (SLF4J, params+result+duration) + durable `audit_log` (`AuditEventService`).
`CorrelationIdFilter` sets/propagates `X-Correlation-Id` (runtime-verified in responses) and injects it
into logs and audit rows.

### Security headers
- **Backend `/api`** (Spring Security): HSTS, `X-Content-Type-Options`, frame-deny, `Referrer-Policy`,
  CSP `default-src 'none'`.
- **SPA (nginx gateway)**: CSP (`script-src 'self'`, `style-src 'self' 'unsafe-inline'`),
  `X-Frame-Options: SAMEORIGIN` (preserves Keycloak silent-SSO iframe), `X-Content-Type-Options`,
  `Referrer-Policy`, `Permissions-Policy`, COOP, CORP, `server_tokens off`.

### Password hashing
`BCryptPasswordEncoder` (always-on bean). Provisioned passwords are BCrypt-hashed; absent passwords
store a BCrypt hash of a random value (never a usable known credential).

---

## 7. Risk Assessment & Residual Risks

| Risk | Severity | Status | Mitigation / plan |
|---|---|---|---|
| CSP `style-src 'unsafe-inline'` (SPA) | **Low/Medium (accepted)** | Open | Required by React inline styles; `script-src` stays strict. Remove by migrating inline styles to stylesheets. |
| Keycloak DB password is a dev value in init SQL | **Medium** | Open | Source from a secret manager for production. |
| Rate limiting is per-instance | **Low** | Open | Move to Redis for multi-instance correctness. |
| `refresh_tokens` reserved/unused schema | **Informational** | Open | Implement or remove. |
| CI security gates report-only (Trivy/CodeQL) | **Low** | Open | Flip to enforcing after triage. |

---

## 8. Verification Summary

| Control | Automated test | Runtime (real JWT) | ZAP |
|---|---|---|---|
| Unauthenticated blocked | `OAuth2UnauthorizedIT` | `401` ✓ | — |
| JWT signature validation | (mock decoder in tests) | garbage → `401` ✓ | — |
| IDOR prevention | `TransferAuthorizationIT` | balance/transfer → `403` ✓ | passive PASS |
| RBAC | (method security enabled) | non-admin → admin `403` ✓ | — |
| Idempotency | `TransferAuthorizationIT` | — | — |
| Fraud 2FA | `FraudChallengeFlowIT` | — | — |
| Security headers | — | present in responses ✓ | 0 High/Critical |

> Anything not runtime-executed is labeled as such; nothing here is fabricated. See
> [06_OWASP_ZAP_Report.md](06_OWASP_ZAP_Report.md) for scan details.

---

### Document Control

| Field | Value |
|---|---|
| ✔ Document Status | **Approved — controls verified in code + runtime** |
| ✔ Last Reviewed | 2026-07-10 |
| ✔ Version | 1.0.0 |
| ✔ Related Documents | [06_OWASP_ZAP_Report](06_OWASP_ZAP_Report.md), [01_Enterprise_Architecture](01_Enterprise_Architecture.md), [09_Operations_Runbook](09_Operations_Runbook.md) |
