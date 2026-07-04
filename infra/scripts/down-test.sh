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

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" down
echo "Test environment stopped."
