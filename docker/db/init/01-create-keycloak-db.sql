-- Dedicated database + user for Keycloak, isolated from the application database.
CREATE DATABASE keycloak_db;
CREATE USER keycloak WITH PASSWORD 'keycloak';
GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO keycloak;

-- PostgreSQL 15+ no longer grants CREATE on schema public to non-owners, so a database-level grant
-- alone leaves Keycloak with "permission denied for schema public" on first start. Make the keycloak
-- role own the database and its public schema so Liquibase can create its tables.
ALTER DATABASE keycloak_db OWNER TO keycloak;
\connect keycloak_db
GRANT ALL ON SCHEMA public TO keycloak;
ALTER SCHEMA public OWNER TO keycloak;
