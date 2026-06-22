#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKUP_DIR="$DEPLOY_DIR/backups"
TIMESTAMP="$(date -u +%Y%m%dT%H%M%SZ)"

cd "$DEPLOY_DIR"
mkdir -p "$BACKUP_DIR"

set -a
source .env
set +a

docker compose --env-file .env exec -T postgres \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --format=custom \
  > "$BACKUP_DIR/postgres-$TIMESTAMP.dump"

find "$BACKUP_DIR" -type f -name 'postgres-*.dump' -mtime +7 -delete

echo "Backup criado em $BACKUP_DIR/postgres-$TIMESTAMP.dump"
