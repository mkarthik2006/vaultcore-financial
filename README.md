# VaultCore Financial

[![CI](https://github.com/mkarthik2006/vaultcore-financial/actions/workflows/ci.yml/badge.svg?branch=feature/enterprise-hardening)](https://github.com/mkarthik2006/vaultcore-financial/actions/workflows/ci.yml)
![Tests](https://img.shields.io/badge/tests-35%20passing-brightgreen)
![Coverage](https://img.shields.io/badge/coverage-76.9%25%20lines-brightgreen)
![OWASP ZAP](https://img.shields.io/badge/OWASP%20ZAP-0%20High%2FCritical-brightgreen)
![Status](https://img.shields.io/badge/status-Enterprise%20Ready-success)
<br/>
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F)
![React](https://img.shields.io/badge/React-19-61DAFB)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791)
![Redis](https://img.shields.io/badge/Redis-7-DC382D)
![Keycloak](https://img.shields.io/badge/Keycloak-24-4D4D4D)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)

Enterprise-grade FinTech platform built for **Zaalima Development – Q4 High-Performance Java (Project-1)**.  
This project implements secure digital banking and portfolio operations using a cloud-native, containerized architecture.

📚 **Full documentation portal:** [`docs/README.md`](docs/README.md) — architecture, API, database, security, OWASP ZAP, testing, deployment, operations, and release notes.

---

## Project Alignment

This repository aligns with **Project 1: FinTech – Secure Digital Banking & Trading Core**:

- Security-first architecture (OAuth2/OIDC with external IdP)
- ACID-oriented transaction handling
- Immutable-ledger foundation
- Java 21 modern runtime capabilities (Virtual Threads enabled)
- Dockerized, production-style local deployment

---

## Tech Stack

### Backend
- Java 21
- Spring Boot 3.x
- Spring Security 6 (OAuth2 Resource Server / JWT)
- Spring Data JPA + Hibernate 6
- Flyway migrations

### Frontend
- React (Vite)
- Keycloak JS integration

### Data / Infra
- PostgreSQL 15
- Redis 7
- Keycloak 24
- Nginx Gateway
- Docker Compose

---

## High-Level Architecture

Client requests enter through **Nginx Gateway (`:8082`)** and are routed as follows:

- `/` -> Frontend container
- `/api/*` -> Backend container
- `/auth/*`, `/realms/*`, `/admin/*`, `/resources/*` -> Keycloak container
- `/health.json` -> Gateway health response

Core services:
- `vaultcore-db`
- `vaultcore-keycloak`
- `vaultcore-redis`
- `vaultcore-stock-mock`
- `vaultcore-app`
- `vaultcore-frontend`
- `vaultcore-gateway`

---

## Repository Structure

- `docker-compose.yml` - Multi-container orchestration
- `docker/gateway/default.conf` - Gateway routing config
- `docker/keycloak/realm-import/` - Keycloak realm import
- `src/main/resources/application.yaml` - Backend config
- `src/main/resources/db/migration/` - Flyway SQL migrations
- `frontend/src/services/` - API and auth clients

---

## Week-Wise Implementation Summary

### Week 1 – Security & Immutable Ledger Foundation
- OIDC/JWT security integration with Keycloak
- Initial schema and migration setup via Flyway
- Security-focused project foundation

### Week 2 – Transaction Engine
- Transfer/account flow services implemented
- Transaction consistency focus (ACID handling)

### Week 3 – Trading & External API
- Mock stock API integration
- Portfolio service + frontend API integration

### Week 4 – Audit & Compliance
- Compliance-focused branch work merged
- Docker startup reliability and health checks stabilized
- Gateway + auth + API integration hardened

---

## Security & Compliance Highlights

- Externalized identity and authentication with Keycloak
- JWT validation on protected backend endpoints
- Environment-driven configuration for sensitive values
- Gateway-mediated service exposure
- Health probes and operational checks integrated

---

## Virtual Threads (Project Loom) Evidence

Virtual threads are enabled in backend configuration:

- File: `src/main/resources/application.yaml`
- Property: `spring.threads.virtual.enabled: true`

---

## Run Locally

## Prerequisites
- Docker Desktop installed and running
- `.env` file configured

## Start
```bash
docker compose down -v
docker compose up -d --build
```

## Verify
```bash
docker ps -a
curl -s http://localhost:8082/health.json
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8082/api/v1/portfolio
```

Expected:
- `{"status":"UP"}`
- `401` for unauthenticated protected endpoint (expected behavior)

## Stop
```bash
docker compose down
```

---

## API Reference

All endpoints are served under the gateway at `http://localhost:8082` and require a Keycloak-issued
bearer token unless noted. Money-movement and balance endpoints enforce **account ownership** — a
caller may only act on accounts they own.

| Method | Path | Auth | Notes |
|---|---|---|---|
| `POST` | `/api/v1/transfers` | Owner of `fromAccount` | Body: `{fromAccount,toAccount,amount,currency}`. Optional `Idempotency-Key` header (safe retries). Optional `X-Fraud-Challenge-Id` header to satisfy a 2FA challenge. `201` on success; `403 fraud_challenge_required` when `amount >= threshold`; `409 idempotency_conflict` on key reuse. |
| `GET` | `/api/v1/ledger/balance?accountNumber=&currency=` | Owner of account | Virtual-thread-backed balance read. |
| `POST` | `/api/v1/fraud/challenges/{id}/verify` | Authenticated | Body: `{code}` (6 digits). Verifies a 2FA challenge; then resubmit the transfer with `X-Fraud-Challenge-Id`. |
| `GET` | `/api/v1/portfolio` / `/valuation` | Authenticated | Portfolio DTOs (Recharts-friendly). |
| `POST` | `/api/v1/portfolio/holdings` | Authenticated | Add a holding. |
| `GET` | `/api/v1/statements?month=YYYY-MM` | Authenticated | Monthly PDF statement (multi-page). |
| `POST` | `/api/v1/admin/users` / `/accounts` | `ROLE_ADMIN` | Provisioning; passwords BCrypt-hashed; accounts may be bound to an `ownerUsername`. |

Every response carries an `X-Correlation-Id` (echoed from the request or generated) for tracing.

## Environment Variables

See `docs/RUNBOOK.md` for the full table. Key variables (supply via `.env`):
`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`, `OAUTH2_ISSUER_URI`, `OAUTH2_JWK_SET_URI`, `OAUTH2_AUDIENCE`,
`SPRING_REDIS_HOST/PORT`, `STOCK_API_BASE_URL`, `APP_CORS_ALLOWED_ORIGINS`,
`APP_RATE_LIMIT_ENABLED/RPM`, `FRAUD_CHALLENGE_TTL_SECONDS`, `KEYCLOAK_ADMIN/_PASSWORD`,
`KEYCLOAK_DB_PASSWORD`.

## Database Schema (Flyway)

| Migration | Contents |
|---|---|
| `V1` | `ledger_entries` (immutable via UPDATE/DELETE trigger; deferred double-entry validation trigger; amount/type CHECKs; indexes on `account_id`, `transaction_id`) |
| `V2`/`V3` | `users`, `refresh_tokens` |
| `V4` | `accounts`, `transaction_references` |
| `V5` | portfolio tables |
| `V6` | `accounts.user_id` → `users` (account ownership) |
| `V7` | `idempotency_keys` (UNIQUE key; reserve-before-execute) |
| `V8` | `fraud_challenges` (2FA lifecycle) |
| `V9` | composite `ledger_entries(account_id, created_at)` index (statements/balance) |
| `V10` | `audit_log` (durable audit trail) |

## Testing

```bash
./mvnw verify          # unit + Testcontainers integration tests (requires Docker)
./mvnw test -Dtest='*Test'   # fast unit tests only (no Docker)
```

- **Unit (Mockito, no Docker):** `FraudDetectionServiceTest`, `AccountOwnershipServiceTest`, `LedgerServiceTest`, `StockPriceClientTest`.
- **Integration / security (MockMvc + Testcontainers PostgreSQL):** `TransferAuthorizationIT` (401/IDOR-403/201/idempotency), `FraudChallengeFlowIT` (challenge→verify→resubmit), `TransferServiceConcurrencyIT` (100 threads), `LedgerImmutabilityIT`, `LedgerDoubleEntryIT`, `OAuth2UnauthorizedIT`.
- CI runs the full suite on every push/PR — see `.github/workflows/ci.yml`.

---

## Compliance Documentation

Detailed evaluator-facing evidence is maintained in:

- `docs/COMPLIANCE_EVIDENCE.md`
- `docs/RUNBOOK.md`
- `docs/SECURITY_VALIDATION.md`

---

## Current Status Snapshot

| Requirement | Status |
|---|---|
| Dockerized deployment | ✅ |
| OAuth2/OIDC + JWT security | ✅ |
| Account ownership / IDOR prevention | ✅ |
| Idempotent transfers | ✅ |
| Fraud 2FA challenge + completion | ✅ |
| Durable audit trail + correlation IDs | ✅ |
| PostgreSQL + Redis integration | ✅ |
| Gateway routing | ✅ |
| Virtual threads enabled | ✅ |
| CI (build + tests + image scan) | ✅ |
| Unit + integration + security tests | ✅ |
| OWASP ZAP report attachment | ⚠️ Planned (see `docs/RUNBOOK.md`) |
| Full compliance evidence docs | ✅ |

---

## Submission Links

- Repository: https://github.com/mkarthik2006/vaultcore-financial
- LinkedIn: https://www.linkedin.com/in/karthik-muthuirulappan-333aba320/

---

## Author

**Karthik Muthuirulan**  
GitHub: https://github.com/mkarthik2006  
LinkedIn: https://www.linkedin.com/in/karthik-muthuirulappan-333aba320/
