# VaultCore Financial — Operations Runbook

> Operational procedures for the running stack. Complements [08_Deployment_Guide.md](08_Deployment_Guide.md)
> (build/deploy) and [05_Security_Assessment.md](05_Security_Assessment.md) (controls). A shorter quick
> reference also exists at `docs/RUNBOOK.md`.

---

## 1. Service Map & Ownership

| Service | Role | Restart-safe? | State |
|---|---|---|---|
| `gateway` | Public entry (`:8082`) | yes | stateless |
| `frontend` | SPA | yes | stateless |
| `app` | Business logic + API | yes | stateless (JWT) |
| `keycloak` | IdP | yes | state in `keycloak_db` |
| `db` | PostgreSQL | with care | **stateful** (`vaultcore_pgdata`) |
| `redis` | cache | yes | ephemeral cache |
| `stock-mock-api` | price source | yes | stateless |

---

## 2. Daily Operations

**Health sweep**
```bash
docker compose ps
curl -s http://localhost:8082/health.json
curl -s http://localhost:8082/api/actuator/health
```

**Follow logs (with correlation IDs)**
```bash
docker compose logs -f app | grep correlationId
# investigate one request end-to-end:
docker compose logs app | grep <correlation-id>
```

**Audit review**
```sql
-- inside db: recent security-relevant events
SELECT created_at, action, principal, correlation_id, detail
FROM audit_log ORDER BY created_at DESC LIMIT 100;
```

---

## 3. Startup

```bash
docker compose up -d           # health-gated; converges automatically
```
Order enforced by Compose: `db` → `keycloak` → `redis` → `app` → `frontend`/`gateway`. First start is
slow (Keycloak build/import, Flyway). Confirm with the health sweep above.

## 4. Shutdown

```bash
docker compose stop            # graceful stop, keep data
docker compose down            # remove containers, keep volume
docker compose down -v         # DESTROY data (also drops keycloak_db + app DB)
```

---

## 5. Backup

**PostgreSQL (application + Keycloak DBs)**
```bash
docker compose exec -T db pg_dumpall -U "$POSTGRES_USER" > backup_$(date +%F).sql
# or per-database:
docker compose exec -T db pg_dump -U "$POSTGRES_USER" vaultcore   > vaultcore_$(date +%F).sql
docker compose exec -T db pg_dump -U "$POSTGRES_USER" keycloak_db > keycloak_$(date +%F).sql
```

The ledger is append-only, so point-in-time snapshots are internally consistent.

## 6. Restore

```bash
# fresh volume, then load
docker compose down -v
docker compose up -d db
cat backup_YYYY-MM-DD.sql | docker compose exec -T db psql -U "$POSTGRES_USER" -d postgres
docker compose up -d           # bring up the rest
```

Because Flyway is at `validate`, restore a schema at the **same or newer** migration version than the
running application expects.

---

## 7. Incident Response

### 7.1 Security incident (suspected unauthorized access)
1. **Contain:** scale the `app` to 0 or `docker compose stop app gateway` to halt the API.
2. **Triage with correlation IDs:** pull the offending `X-Correlation-Id` from access logs; join
   `app` logs and `audit_log` on `correlation_id` to reconstruct the action trail.
3. **Assess ledger integrity:** the ledger is immutable — confirm no out-of-band writes; verify
   `SELECT transaction_id, SUM(CASE WHEN entry_type='DEBIT' THEN amount ELSE -amount END) FROM
   ledger_entries GROUP BY transaction_id HAVING SUM(...) <> 0;` returns **no rows**.
4. **Rotate secrets:** Keycloak admin + DB credentials; force token invalidation in Keycloak (logout
   all sessions for the affected realm/user).
5. **Recover:** patch, redeploy, re-run OWASP ZAP baseline; document in the incident log.

### 7.2 Fraud false-positive / stuck challenge
Challenges auto-expire after `FRAUD_CHALLENGE_TTL_SECONDS` (default 300). A new transfer attempt issues
a fresh challenge. No manual DB edit needed.

### 7.3 Idempotency stuck `IN_PROGRESS`
The service releases reservations on recoverable failure. If a key is genuinely stuck (crash mid-flight),
it can be safely deleted:
```sql
DELETE FROM idempotency_keys WHERE idempotency_key = :key AND status = 'IN_PROGRESS';
```

---

## 8. Database Recovery

| Scenario | Action |
|---|---|
| Corrupted app DB | Restore latest dump (§6); Flyway validates schema on startup. |
| Failed migration | Roll **forward** with a new `V{n+1}` migration; never edit applied V1–V10. |
| Balance dispute | Recompute from ledger: `SUM(CREDIT) − SUM(DEBIT)` per `account_id`; the ledger is authoritative. |

## 9. Keycloak Recovery

| Scenario | Action |
|---|---|
| Keycloak won't start (`permission denied for schema public`) | Ensure `keycloak` owns `keycloak_db`/schema (init script); on a stale volume, `down -v` to re-run init. |
| Realm lost | Re-import via `command: start --import-realm` (mounted `docker/keycloak/realm-import`). |
| Admin locked out | Recreate via `KEYCLOAK_ADMIN`/`KEYCLOAK_ADMIN_PASSWORD` env + restart. |
| Admin ops on the running server | Use `kcadm.sh` inside the container against `http://localhost:8080` (HTTPS-exempt on localhost). |

---

## 10. Monitoring, Logs & Metrics

| Signal | Source |
|---|---|
| Liveness/readiness | `/api/actuator/health` |
| Build info | `/api/actuator/info` |
| Request tracing | `X-Correlation-Id` (response header + logs + audit rows) |
| Audit trail | `audit_log` table |
| Container health | `docker compose ps`, healthcheck logs |
| Rate-limit hits | `429` responses / `RateLimitFilter` |

Log format includes `correlationId=` (via `logging.pattern.level`) for correlation across the request
lifecycle.

---

## 11. Disaster Recovery

| RPO/RTO driver | Practice |
|---|---|
| **Data** | Regular `pg_dump` of both DBs to off-host storage; ledger is append-only and snapshot-consistent. |
| **Config** | Everything is in the repo (compose, Dockerfiles, realm export, migrations) — infra is reproducible from source + `.env`. |
| **Rebuild** | `docker compose down -v && docker compose up -d --build` recreates the entire stack from scratch. |
| **Verification after DR** | Health sweep + `mvn verify` (Testcontainers) + OWASP ZAP baseline. |

---

### Document Control

| Field | Value |
|---|---|
| ✔ Document Status | **Approved — reflects running stack** |
| ✔ Last Reviewed | 2026-07-10 |
| ✔ Version | 1.0.0 |
| ✔ Related Documents | [08_Deployment_Guide](08_Deployment_Guide.md), [04_Database_Design](04_Database_Design.md), [05_Security_Assessment](05_Security_Assessment.md) |
