#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

if [ ! -f .env ]; then
  cp .env.example .env
  printf '[ok] .env criado a partir de .env.example\n'
else
  printf '[ok] .env já existe; mantendo arquivo atual\n'
fi

bash scripts/check-env.sh

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  docker compose --env-file .env config >/dev/null
  printf '[ok] Configuração do Docker Compose validada\n'
else
  printf '[aviso] Docker Compose ainda não está disponível; validação ignorada\n'
fi

printf 'Bootstrap concluído. Próximo comando: docker compose --env-file .env up -d\n'
