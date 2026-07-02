#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/.env.prod}"
COMPOSE_FILE="$ROOT_DIR/docker-compose.prod.yml"
APP_ENV_FILE="$ROOT_DIR/.env.app"
DB_ENV_FILE="$ROOT_DIR/.env.db"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "env file not found: $ENV_FILE" >&2
  exit 1
fi

for file in "$APP_ENV_FILE" "$DB_ENV_FILE"; do
  if [[ ! -f "$file" ]]; then
    echo "runtime env file not found: $file" >&2
    exit 1
  fi
done

if grep -Ev '^\s*#|^\s*$' "$ENV_FILE" "$APP_ENV_FILE" "$DB_ENV_FILE" | grep -Eq 'CHANGE_ME|app\\.example\\.com'; then
  echo "placeholder values remain in env files; replace CHANGE_ME/app.example.com before deploy" >&2
  exit 1
fi

ENV_FILES=("$ENV_FILE")
[ -f "$DB_ENV_FILE" ] && ENV_FILES+=(--env-file "$DB_ENV_FILE")

docker compose "${ENV_FILES[@]}" -f "$COMPOSE_FILE" pull app worker
docker compose "${ENV_FILES[@]}" -f "$COMPOSE_FILE" up -d
docker compose "${ENV_FILES[@]}" -f "$COMPOSE_FILE" ps
