-- Remove the unused refresh_tokens table (introduced in V3, never wired to any entity/repository).
-- Token refresh is delegated to Keycloak (the mandated Identity Provider) via the keycloak-js
-- adapter's silent refresh (keycloak.updateToken), so a backend-managed refresh-token store is
-- unnecessary and would only add attack surface. Dropping the dead schema removes the "orphaned
-- migration" gap. (V3 is retained as applied history; this is the forward-only removal.)
DROP TABLE IF EXISTS refresh_tokens;
