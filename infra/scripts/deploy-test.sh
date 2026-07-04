#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="/srv/keepgoing-test"
ENV_FILE="${1:-$ROOT_DIR/.env.test}"
COMPOSE_FILE="$ROOT_DIR/docker-compose.test.yml"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "env file not found: $ENV_FILE" >&2
  exit 1
fi

cd "$ROOT_DIR"

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull test-app test-worker
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps

# Health check
echo "Waiting for test-app health check..."
for i in $(seq 1 30); do
  if curl -fsS http://127.0.0.1:8082/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
    echo "test-app is UP!"
    break
  fi
  if [ "$i" -eq 30 ]; then
    echo "test-app health check timed out" >&2
    exit 1
  fi
  sleep 2
done
