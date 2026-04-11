#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
ENV_FILE="${1:-$ROOT_DIR/.env.prod}"
COMPOSE_FILE="$ROOT_DIR/docker-compose.prod.yml"
DB_ENV_FILE="$ROOT_DIR/.env.db"
BACKUP_DIR="${BACKUP_DIR:-$ROOT_DIR/backups}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "env file not found: $ENV_FILE" >&2
  exit 1
fi

if [[ ! -f "$DB_ENV_FILE" ]]; then
  echo "db env file not found: $DB_ENV_FILE" >&2
  exit 1
fi

mkdir -p "$BACKUP_DIR"

set -a
source "$ENV_FILE"
source "$DB_ENV_FILE"
set +a

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
OUTPUT_FILE="$BACKUP_DIR/postgres-${POSTGRES_DB}-${TIMESTAMP}.sql.gz"

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T postgres \
  pg_dump -U "$POSTGRES_USER" "$POSTGRES_DB" \
  | gzip > "$OUTPUT_FILE"

echo "backup created: $OUTPUT_FILE"
