CREATE DATABASE keycloak_db;
CREATE USER keycloak WITH PASSWORD 'keycloak';
GRANT ALL PRIVILEGES ON DATABASE keycloak_db TO keycloak;