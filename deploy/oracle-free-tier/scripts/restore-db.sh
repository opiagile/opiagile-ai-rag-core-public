#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Uso: $0 caminho/do/backup.dump"
  exit 1
fi

BACKUP_FILE="$1"
DEPLOY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if [[ ! -f "$BACKUP_FILE" ]]; then
  echo "Backup não encontrado: $BACKUP_FILE"
  exit 1
fi

cd "$DEPLOY_DIR"

set -a
source .env
set +a

docker compose --env-file .env exec -T postgres \
  pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists \
  < "$BACKUP_FILE"

echo "Restore concluído a partir de $BACKUP_FILE"
