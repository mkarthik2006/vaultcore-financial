# OWASP ZAP Baseline Scan — VaultCore Financial

- **Target:** `http://localhost:8082` (nginx gateway → SPA / API / Keycloak)
- **Scanner:** OWASP ZAP baseline (`zaproxy:stable`), passive scan
- **Generated:** Fri, 10 Jul 2026 11:07:13
- **Result:** **0 High · 0 Critical** — no failing (FAIL-NEW) rules; 62 passive rules PASSED

## Severity summary

| Severity | Count |
|---|---|
| High | 0 |
| Medium | 1 |
| Low | 2 |
| Informational | 2 |

## Findings

| Severity | Alert | Instances | CWE |
|---|---|---|---|
| Medium | CSP: style-src unsafe-inline | 2 | 693 |
| Low | Cross-Origin-Embedder-Policy Header Missing or Invalid | 3 | 693 |
| Low | Timestamp Disclosure - Unix | 5 | 497 |
| Informational | Modern Web Application | 3 | -1 |
| Informational | Storable and Cacheable Content | 5 | 524 |

## Disposition

- **High / Critical:** none. ✅ Stop condition met.
- **Medium — `CSP: style-src unsafe-inline`:** *accepted, documented.* The React SPA uses inline `style` attributes, which require `style-src 'unsafe-inline'`. `script-src` remains strict `'self'` (no `unsafe-inline`/`unsafe-eval`), so script-injection XSS is still blocked; the residual risk of style-based injection is low. Eliminating it would require migrating all inline styles to stylesheets.
- **Low — Cross-Origin-Embedder-Policy missing:** intentionally omitted. `COEP: require-corp` can break Keycloak's cross-window/iframe SSO flow; the benefit does not justify the risk for this app. COOP and CORP are set.
- **Low — Timestamp disclosure:** ZAP pattern-matches numeric literals in the minified JS bundle; not a real secret disclosure.
- Prior scan's 2 Mediums (CSP not set, missing anti-clickjacking) and several Lows (X-Content-Type-Options, Permissions-Policy, Server version leak) were **fixed** by adding scoped security headers + `server_tokens off` to the gateway.
