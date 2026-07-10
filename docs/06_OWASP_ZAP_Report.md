# VaultCore Financial — OWASP ZAP Baseline Report

> This report summarizes an **actual** OWASP ZAP baseline scan executed against the running Docker
> stack. Raw artifacts are committed at `security/zap/zap-report.html`, `security/zap/zap-report.json`,
> and `security/zap/ZAP_SUMMARY.md`. No results in this document are fabricated.

---

## 1. Scan Scope

| Field | Value |
|---|---|
| Target | `http://host.docker.internal:8082` (nginx gateway → SPA / `/api` / Keycloak) |
| Scanner | OWASP ZAP (`ghcr.io/zaproxy/zaproxy:stable`), `zap-baseline.py` |
| Scan type | **Baseline** (spider + passive rules); no active/intrusive attacks |
| Authentication | Unauthenticated (baseline crawls the public surface) |
| Environment | Live `docker compose` stack, all services healthy |
| Report generated | Fri, 10 Jul 2026 |

The baseline scan crawls the SPA, static assets, the `/api` surface (which returns `401` when
unauthenticated), and Keycloak's login endpoints, then applies passive rules (headers, cookies,
information disclosure, etc.).

---

## 2. Execution

```bash
# stack already running: docker compose up -d  (all services healthy)
docker run --rm -v "$PWD/security/zap:/zap/wrk/:rw" ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py -t http://host.docker.internal:8082 \
  -r zap-report.html -J zap-report.json
```

The scan was run, findings were remediated (gateway security headers), and the scan was **re-run** to
confirm the fixes.

---

## 3. Results (final scan)

| Severity | Count |
|---|---|
| **Critical** | **0** |
| **High** | **0** |
| Medium | 1 |
| Low | 2 |
| Informational | 2 |

Summary line: `FAIL-NEW: 0  FAIL-INPROG: 0  WARN-NEW: 5  PASS: 62`.

### Before → after remediation

| Finding | Initial | Final |
|---|---|---|
| Content Security Policy (CSP) not set | Medium (x3) | **Fixed** |
| Missing Anti-clickjacking header | Medium (x3) | **Fixed** |
| X-Content-Type-Options missing | Low (x5) | **Fixed** |
| Permissions-Policy not set | Low | **Fixed** |
| Server version leak (nginx) | Low | **Fixed** (`server_tokens off`) |
| CSP `style-src 'unsafe-inline'` | — | Medium (**accepted**) |

---

## 4. Findings Detail

### 4.1 Critical / High
**None.** ✅ This satisfies the release gate (no Critical or High findings).

### 4.2 Medium — `CSP: style-src unsafe-inline` (accepted risk)
- **Where:** SPA root and static responses.
- **Cause:** the CSP intentionally allows `style-src 'unsafe-inline'` because the React application
  uses inline `style` attributes.
- **Why accepted:** `script-src` remains strict `'self'` (no `unsafe-inline`/`unsafe-eval`), so
  script-injection XSS is still blocked; the residual risk of style-based injection is low.
- **Remediation path:** migrate inline styles to stylesheets/CSS modules, then tighten to
  `style-src 'self'`.

### 4.3 Low
| Finding | Disposition |
|---|---|
| Cross-Origin-Embedder-Policy missing | **Intentionally omitted** — `COEP: require-corp` risks breaking Keycloak's cross-window/iframe SSO. COOP and CORP are set. |
| Timestamp disclosure (Unix) | Pattern-match on numeric literals in the minified JS bundle; not a real secret disclosure. |

### 4.4 Informational
Standard ZAP informational notices (e.g., modern-browser / storable-content). No action required.

---

## 5. Accepted Risks Register

| Risk | Severity | Owner | Review |
|---|---|---|---|
| CSP `style-src 'unsafe-inline'` | Medium | Frontend | revisit when inline styles are removed |
| COEP header omitted | Low | Platform | revisit if COEP-dependent features are added |

---

## 6. Screenshots (captions — artifacts committed, not embedded)

> The HTML report at `security/zap/zap-report.html` renders these views; open it in a browser.

- **Figure 6-1** — ZAP baseline summary: `PASS: 62`, `FAIL-NEW: 0`, 0 High/Critical.
- **Figure 6-2** — Alerts table showing the single accepted Medium (`style-src unsafe-inline`).
- **Figure 6-3** — Before/after: CSP and anti-clickjacking cleared after gateway header remediation.

---

## 7. Overall Assessment

The application passes the OWASP ZAP baseline gate: **0 Critical, 0 High, 0 FAIL-NEW**, with 62 passive
rules passing. The single remaining Medium is a documented, accepted trade-off inherent to the SPA's
styling approach and does not represent an exploitable script-injection path. The result is consistent
with the code-level security controls in [05_Security_Assessment.md](05_Security_Assessment.md).

> **Note on scope:** A baseline (passive) scan does not perform active injection/attack testing. For a
> production go-live, a full authenticated **active** ZAP scan (with a logged-in session context) is
> recommended as a follow-up; the baseline result is the current, evidenced state.

---

### Document Control

| Field | Value |
|---|---|
| ✔ Document Status | **Approved — reflects committed scan artifacts** |
| ✔ Last Reviewed | 2026-07-10 |
| ✔ Version | 1.0.0 |
| ✔ Related Documents | [05_Security_Assessment](05_Security_Assessment.md), [07_Test_Strategy_and_Coverage](07_Test_Strategy_and_Coverage.md) |
