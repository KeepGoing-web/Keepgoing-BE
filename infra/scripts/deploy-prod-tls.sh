#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/.env.prod}"
BASE_COMPOSE_FILE="$ROOT_DIR/docker-compose.prod.yml"
TLS_COMPOSE_FILE="$ROOT_DIR/docker-compose.prod.tls.yml"
APP_ENV_FILE="$ROOT_DIR/.env.app"
DB_ENV_FILE="$ROOT_DIR/.env.db"

for file in "$ENV_FILE" "$APP_ENV_FILE" "$DB_ENV_FILE"; do
  if [[ ! -f "$file" ]]; then
    echo "required env file not found: $file" >&2
    exit 1
  fi
done

if grep -Ev '^\s*#|^\s*$' "$ENV_FILE" "$APP_ENV_FILE" "$DB_ENV_FILE" | grep -Eq 'CHANGE_ME|app\\.example\\.com'; then
  echo "placeholder values remain in env files; replace CHANGE_ME/app.example.com before deploy" >&2
  exit 1
fi

docker compose \
  --env-file "$ENV_FILE" \
  -f "$BASE_COMPOSE_FILE" \
  -f "$TLS_COMPOSE_FILE" \
  up -d --build

docker compose \
  --env-file "$ENV_FILE" \
  -f "$BASE_COMPOSE_FILE" \
  -f "$TLS_COMPOSE_FILE" \
  ps
