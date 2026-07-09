# VaultCore Financial — Runbook

## Prerequisites
- Docker Desktop (or a Docker daemon) running.
- A `.env` file at the repo root (see **Environment variables** below).

## Run the full stack
```bash
docker compose down -v          # clean slate (drops volumes)
docker compose up -d --build    # build & start all services
```
The application is served through the nginx gateway at **http://localhost:8082**.

| Path | Routed to |
|---|---|
| `/`            | Frontend SPA |
| `/api/*`       | Backend (Spring Boot) |
| `/auth/*`, `/realms/*`, `/admin/*`, `/resources/*` | Keycloak |

Verify health:
```bash
curl -s http://localhost:8082/health.json          # gateway
curl -s http://localhost:8082/api/actuator/health  # backend (via gateway)
```

## Run the test suite (requires Docker for Testcontainers)
```bash
./mvnw verify          # unit + integration tests (PostgreSQL via Testcontainers)
```
Key suites: `TransferServiceConcurrencyIT` (100-thread correctness), `LedgerImmutabilityIT`,
`LedgerDoubleEntryIT`, `TransferAuthorizationIT` (IDOR + idempotency), `FraudChallengeFlowIT`
(2FA completion), `OAuth2UnauthorizedIT`.

## Frontend
```bash
cd frontend
npm ci
npm run dev     # local dev server
npm run build   # production build
```

## Environment variables
| Variable | Purpose |
|---|---|
| `SPRING_DATASOURCE_URL/USERNAME/PASSWORD` | Application database connection |
| `OAUTH2_ISSUER_URI` / `OAUTH2_JWK_SET_URI` | Keycloak realm / JWKS |
| `OAUTH2_AUDIENCE` | Optional JWT audience to enforce |
| `SPRING_REDIS_HOST` / `SPRING_REDIS_PORT` | Redis (cache) |
| `STOCK_API_BASE_URL` | Mock stock price API base URL |
| `APP_CORS_ALLOWED_ORIGINS` | Comma-separated allowed browser origins (default gateway) |
| `FRAUD_CHALLENGE_TTL_SECONDS` | Fraud 2FA challenge validity (default 300) |
| `KEYCLOAK_ADMIN` / `KEYCLOAK_ADMIN_PASSWORD` | Keycloak bootstrap admin |
| `KEYCLOAK_DB_PASSWORD` | Keycloak database password |

## OWASP ZAP baseline scan (Week-4 requirement)
With the stack running:
```bash
docker run --rm --network host ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py -t http://localhost:8082 -r zap-report.html
```
Attach `zap-report.html` to the submission and remediate any HIGH findings.

## Common operations
- **Tail backend logs:** `docker compose logs -f backend`
- **Recreate DB from scratch:** `docker compose down -v && docker compose up -d`
- **Fraud challenge stuck:** challenges expire after `FRAUD_CHALLENGE_TTL_SECONDS`; a new transfer
  attempt issues a fresh one.
