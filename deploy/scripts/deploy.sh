#!/usr/bin/env bash
# Build + (re)start the VaultCore stack with the production overrides, health-check the gateway,
# and roll back to the previous images if the new build is unhealthy.
# Run from the repo root on the VM:  bash deploy/scripts/deploy.sh
set -euo pipefail

cd "$(dirname "$0")/../.."   # repo root
COMPOSE="docker compose -f docker-compose.yml -f deploy/docker-compose.prod.yml"
HEALTH_URL="http://127.0.0.1:8082/health.json"

if [ ! -f .env ]; then echo "ERROR: .env missing (copy deploy/env/.env.prod.example)"; exit 1; fi

echo "==> Recording current image ids (for rollback)"
PREV=$(mktemp)
docker images --format '{{.Repository}}:{{.Tag}} {{.ID}}' | grep -E 'vaultcore' > "$PREV" || true

echo "==> Pulling latest code"
git fetch --all --prune
git reset --hard "@{u}"

echo "==> Building + starting stack"
$COMPOSE up -d --build

echo "==> Waiting for gateway health (up to ~4 min for first boot / Keycloak import)"
ok=false
for i in $(seq 1 48); do
  if curl -fsS "$HEALTH_URL" 2>/dev/null | grep -q 'UP'; then ok=true; break; fi
  sleep 5
done

if [ "$ok" = true ]; then
  echo "==> Deploy healthy."
  $COMPOSE ps
  docker image prune -f >/dev/null 2>&1 || true
  exit 0
fi

echo "!! Deploy UNHEALTHY — rolling back to previous commit + images"
$COMPOSE logs --tail=80 backend gateway keycloak || true
git reset --hard 'HEAD@{1}' || true
$COMPOSE up -d --build
echo "Rolled back. Investigate logs above."
exit 1
