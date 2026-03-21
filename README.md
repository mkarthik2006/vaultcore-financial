# VaultCore Financial

Enterprise-grade FinTech platform built for **Zaalima Development – Q4 High-Performance Java (Project-1)**.  
This project implements secure digital banking and portfolio operations using a cloud-native, containerized architecture.

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
| PostgreSQL + Redis integration | ✅ |
| Gateway routing | ✅ |
| Virtual threads enabled | ✅ |
| Test/report quantification | ⚠️ In progress |
| OWASP ZAP report attachment | ⚠️ Planned |
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
