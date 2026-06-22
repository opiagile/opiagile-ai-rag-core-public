#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
DEPLOY_DIR="$ROOT_DIR/deploy/oracle-free-tier"

cd "$ROOT_DIR"
git fetch origin develop
git checkout develop
git pull --ff-only origin develop

cd "$DEPLOY_DIR"
docker compose --env-file .env pull postgres caddy
docker compose --env-file .env up -d --build
docker image prune -f
