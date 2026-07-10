#!/usr/bin/env bash
# Nightly backup of both PostgreSQL databases (application + Keycloak) with retention.
# Cron (2am daily):  0 2 * * *  /home/ubuntu/vaultcore-financial/deploy/scripts/backup.sh >> /var/log/vaultcore-backup.log 2>&1
set -euo pipefail

cd "$(dirname "$0")/../.."
set -a; [ -f .env ] && . ./.env; set +a

BACKUP_DIR="${BACKUP_DIR:-/var/backups/vaultcore}"
RETENTION_DAYS="${RETENTION_DAYS:-14}"
STAMP="$(date +%F_%H%M%S)"
sudo mkdir -p "$BACKUP_DIR"

echo "[$(date)] Backing up databases to $BACKUP_DIR"

# Application DB
docker exec vaultcore-db pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" \
  | gzip | sudo tee "$BACKUP_DIR/app_${STAMP}.sql.gz" >/dev/null

# Keycloak DB (owned by the keycloak role)
docker exec vaultcore-db pg_dump -U "$POSTGRES_USER" "$KEYCLOAK_DB" \
  | gzip | sudo tee "$BACKUP_DIR/keycloak_${STAMP}.sql.gz" >/dev/null

echo "[$(date)] Pruning backups older than ${RETENTION_DAYS} days"
sudo find "$BACKUP_DIR" -name '*.sql.gz' -mtime +"$RETENTION_DAYS" -delete

echo "[$(date)] Backup complete:"
sudo ls -lh "$BACKUP_DIR" | tail -n +1

# --- Restore (manual) ---
#   gunzip -c app_YYYY-MM-DD_HHMMSS.sql.gz | docker exec -i vaultcore-db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
# For a full DR from empty volume, restore Keycloak DB the same way after `docker compose up -d db`.
