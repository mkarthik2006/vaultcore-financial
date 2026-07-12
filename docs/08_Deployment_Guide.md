# VaultCore Financial — Deployment Guide

> Reflects `docker-compose.yml`, the root `Dockerfile`, `frontend/Dockerfile`,
> `docker/mock-stock-api/Dockerfile`, `docker/gateway/default.conf`, and
> `docker/db/init/01-create-keycloak-db.sql`. Deployment is a single command.

---

## 1. Prerequisites

| Tool | Version | Needed for |
|---|---|---|
| Docker Desktop / Engine | current (Compose v2) | running the stack |
| RAM available to Docker | **≥ 5 GB** | Keycloak's Quarkus build/augmentation is memory-intensive; ~4 GB is too tight |
| Java | 21 | local (non-Docker) build/test only |
| Maven | wrapper `./mvnw` | local build/test only |
| Node | 20 | local frontend dev only |

> **Operational note:** during bring-up validation, Keycloak was observed to OOM-crash when Docker had
> only ~3.8 GB; raising Docker's memory to ≥ 5 GB resolved it. Size the host accordingly.

---

## 2. Environment Variables (`.env` at repo root — gitignored)

Create `.env` with (dev values shown):

```dotenv
POSTGRES_DB=vaultcore
POSTGRES_USER=vaultuser
POSTGRES_PASSWORD=vaultpass

KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_DB=keycloak_db
KEYCLOAK_DB_USER=keycloak
KEYCLOAK_DB_PASSWORD=keycloak
KEYCLOAK_DB_SCHEMA=public

OAUTH2_ISSUER_URI=http://vaultcore-keycloak:8080/realms/vaultcore
OAUTH2_JWK_SET_URI=http://vaultcore-keycloak:8080/realms/vaultcore/protocol/openid-connect/certs
```

> The Keycloak DB init script (`docker/db/init/01-create-keycloak-db.sql`) creates `keycloak_db` and a
> `keycloak` role that **owns the database and its `public` schema** — required on PostgreSQL 15, which
> no longer grants `CREATE` on `public` to non-owners. **For production, source `KEYCLOAK_DB_PASSWORD`
> from a secret manager.**

---

## 3. Services (from `docker-compose.yml`)

| Service | Container | Host port | Health | Depends on |
|---|---|---|---|---|
| `db` | vaultcore-db | — | `pg_isready` | — |
| `keycloak` | vaultcore-keycloak | — | TCP :8080 | db (healthy) |
| `redis` | vaultcore-redis | `6380→6379` | `redis-cli ping` | — |
| `stock-mock-api` | vaultcore-stock-mock | `8081→8080` | — | — |
| `app` (backend) | vaultcore-app | — | `/actuator/health` | db, keycloak, redis (healthy); stock (started) |
| `frontend` | vaultcore-frontend | — | — | app (started) |
| `gateway` | vaultcore-gateway | **`8082→80`** | `wget 127.0.0.1/health.json` | app, keycloak, frontend |

The **only published entry point** is the gateway on `:8082`.

---

## 4. Build

The backend image is a **multi-stage** build (Maven → JRE) and runs as a **non-root** user; the
frontend builds via Vite into an nginx image. No manual build step is required — Compose builds on `up`.

```bash
# optional: verify locally before deploying
./mvnw clean verify        # 35 tests, Testcontainers (needs Docker)
cd frontend && npm ci && npm run build && cd ..
```

---

## 5. Run

```bash
docker compose down -v          # clean slate (drops volumes so DB init re-runs)
docker compose up -d --build    # build images and start the stack
```

Startup is health-gated and converges automatically. First start takes several minutes (Keycloak build
+ realm import + backend Flyway).

---

## 6. Verification

```bash
docker compose ps                                   # all healthchecked services 'healthy'
curl -s http://localhost:8082/health.json           # {"status":"UP"}
curl -s -o /dev/null -w "%{http_code}\n" \
     http://localhost:8082/api/v1/portfolio          # 401 (unauthenticated, expected)
curl -s -o /dev/null -w "%{http_code}\n" \
     "http://localhost:8082/realms/vaultcore/.well-known/openid-configuration"  # 200
```

Expected: gateway `UP`, frontend `200`, unauthenticated API `401`, Keycloak OIDC discovery `200`.

---

## 7. Health Checks & Monitoring

| Endpoint | Purpose |
|---|---|
| `GET /api/actuator/health` | backend readiness/liveness |
| `GET /health.json` (gateway) | gateway self-health |
| `docker compose ps` | per-service health state |
| Logs | `docker compose logs -f <service>` (backend logs carry `correlationId`) |

Metrics/observability beyond actuator (Prometheus/Grafana) are future work; correlation IDs are already
emitted for request tracing.

---

## 8. Rollback

```bash
# revert to a previous image tag / commit and redeploy
git checkout <previous-tag>
docker compose up -d --build
# data: PostgreSQL volume 'vaultcore_pgdata' persists across redeploys.
# Flyway migrations are append-only; roll forward with a new migration rather than editing V1–V10.
```

For a full reset (destroys data): `docker compose down -v`.

---

## 9. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| Keycloak restarts / `Killed` | OOM (Docker RAM too low) | Give Docker ≥ 5 GB |
| Keycloak `permission denied for schema public` | stale DB volume from before the PG15 fix | `docker compose down -v` to re-run init |
| `Bind for 0.0.0.0:6379 failed` | another Redis on host 6379 | stack now uses host `6380`; stop the conflicting service if needed |
| Gateway `unhealthy` but site works | healthcheck used `localhost`→IPv6 | fixed to `127.0.0.1` (already in compose) |
| API `401` with a token | issuer mismatch (`OAUTH2_ISSUER_URI` vs token `iss`) | ensure issuer matches how tokens are issued |
| API `403` on your own transfer | account has no owner / not owned by caller | provision the account with `ownerUsername` |

---

### Document Control

| Field | Value |
|---|---|
| ✔ Document Status | **Approved — reflects compose/Dockerfiles** |
| ✔ Last Reviewed | 2026-07-10 |
| ✔ Version | 1.0.0 |
| ✔ Related Documents | [09_Operations_Runbook](09_Operations_Runbook.md), [01_Enterprise_Architecture](01_Enterprise_Architecture.md) |
