#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ]; then
  printf 'Uso: %s /caminho/arquivo-openai.env\n' "$0" >&2
  exit 2
fi

SOURCE_ENV="$1"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
TARGET_ENV="$ROOT_DIR/deploy/oracle-free-tier/.env"

if [ ! -f "$SOURCE_ENV" ]; then
  printf 'Arquivo de configuração OpenAI não encontrado.\n' >&2
  exit 1
fi

if [ ! -f "$TARGET_ENV" ]; then
  printf 'Arquivo .env do deploy não encontrado.\n' >&2
  exit 1
fi

python3 - "$SOURCE_ENV" "$TARGET_ENV" <<'PY'
import sys
from pathlib import Path

source_env = Path(sys.argv[1])
target_env = Path(sys.argv[2])

updates = {}
for line in source_env.read_text().splitlines():
    if not line or line.lstrip().startswith("#") or "=" not in line:
        continue
    name, value = line.split("=", 1)
    updates[name] = value

lines = target_env.read_text().splitlines()
seen = set()
out = []

for line in lines:
    if not line or line.lstrip().startswith("#") or "=" not in line:
        out.append(line)
        continue
    name = line.split("=", 1)[0]
    if name in updates:
        out.append(f"{name}={updates[name]}")
        seen.add(name)
    else:
        out.append(line)

for name, value in updates.items():
    if name not in seen:
        out.append(f"{name}={value}")

target_env.write_text("\n".join(out) + "\n")
PY

rm -f "$SOURCE_ENV"
printf 'Configuração OpenAI aplicada a partir do GitHub Secrets.\n'
