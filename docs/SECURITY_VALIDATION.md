# VaultCore Financial — Security Validation

This document records the security controls implemented in the codebase and how each is verified.
It reflects the state of the repository as of the enterprise-hardening work (feature branch
`feature/enterprise-hardening`).

## Authentication
- OAuth2 Resource Server validating Keycloak-issued JWTs (`SecurityConfig`), with issuer, optional
  audience, and clock-skew validators (`NimbusJwtDecoder`).
- Unauthenticated requests to protected endpoints return `401` — verified by `OAuth2UnauthorizedIT`.

## Authorization (object- and function-level)
| Control | Implementation | Verification |
|---|---|---|
| Account ownership (IDOR prevention) | `CurrentUserProvider` resolves the caller from the JWT; `AccountOwnershipService.requireOwnedAccount` enforces ownership on transfer (`fromAccount`) and balance reads via an owner-scoped query | `TransferAuthorizationIT`: cross-account transfer → `403 access_denied`; owned transfer → `201` |
| Function-level (admin) | `@PreAuthorize("hasRole('ADMIN')")` on `AdminProvisioningController` **plus** the URL rule `/api/v1/admin/** → hasRole('ADMIN')` | Method security is enabled in ALL profiles via `MethodSecurityConfig`, so it is enforced under the test profile |
| Non-disclosure | Ownership failures return a generic `403` that does not reveal whether the account exists | `AccountOwnershipService` uses `findByAccountNumberAndOwner_Id`; `GlobalExceptionHandler` returns a generic body |

## Money-movement safety
- **Idempotency:** transfers accept an `Idempotency-Key` header; a reservation row (`idempotency_keys`,
  UNIQUE index) is inserted before execution so concurrent duplicates cannot both post, and replays
  return the original result. Verified by `TransferAuthorizationIT#repeatedIdempotencyKeyExecutesTransferOnlyOnce`.
- **Concurrency:** `SERIALIZABLE` isolation + pessimistic locks + deterministic lock ordering +
  serialization-conflict retry. Verified by `TransferServiceConcurrencyIT` (100 threads).
- **Fraud 2FA:** transfers `>=` threshold require a verified challenge (see below).

## Fraud detection (AOP) & 2FA completion
- Implemented as a Spring AOP interceptor (`FraudDetectionAspect`) on the transfer execution
  primitive.
- Challenges are persisted (`fraud_challenges`), delivered via the mock channel with a **BCrypt-hashed
  code**, verified via `POST /api/v1/fraud/challenges/{id}/verify`, expire after a TTL, and are
  consumed on resubmission (`X-Fraud-Challenge-Id`). Verified end-to-end by `FraudChallengeFlowIT`.

## Credential storage
- Local credentials are BCrypt-hashed (`PasswordEncoder`); the previous plaintext placeholder was
  removed (`AdminProvisioningController`).

## Transport / browser hardening
- Security headers: HSTS, `X-Content-Type-Options`, frame-deny, `Referrer-Policy`, CSP (`SecurityConfig`).
- Explicit CORS policy (`app.cors.allowed-origins`), credentials-aware, restricted methods/headers.
- CSRF disabled (correct for stateless bearer-token API), stateless sessions.

## Input validation & error hygiene
- Bean Validation on all request DTOs; `MethodArgumentNotValidException` → structured `400`.
- No stack traces or internal details leaked; exception advices return message-only JSON.

## Injection
- All persistence via JPA/JPQL/parameterized queries; no string-concatenated SQL.
- React output is auto-escaped; no `dangerouslySetInnerHTML`.

## Penetration testing (OWASP ZAP)
- CI builds the backend image and runs a Trivy vulnerability scan (report-only initially).
- **Outstanding:** a full OWASP ZAP baseline scan against the running `docker compose` stack is still
  required to close the Week-4 review criterion. See `RUNBOOK.md` for how to run it. Track in the
  gap analysis.

## Known residual items (tracked, not yet closed)
- Persistent, tamper-evident audit table + correlation IDs (currently SLF4J audit logs only).
- Rate limiting on auth/transfer endpoints (Redis counters).
- Redis-backed Hibernate L2 cache entity annotations; connection-pool sizing.
