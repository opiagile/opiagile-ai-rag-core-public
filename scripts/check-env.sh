#!/usr/bin/env bash
set -u

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

status=0

check_cmd() {
  local name="$1"
  local cmd="$2"
  local hint="$3"

  if command -v "$cmd" >/dev/null 2>&1; then
    printf '[ok]   %s: %s\n' "$name" "$(command -v "$cmd")"
  else
    printf '[aviso] %s não encontrado. %s\n' "$name" "$hint"
    status=1
  fi
}

printf 'Diagnóstico de ambiente do Acelerador de Atendimento com IA e RAG\n'
printf 'Raiz: %s\n' "$ROOT_DIR"
printf 'Arquitetura: %s\n' "$(uname -m 2>/dev/null || printf desconhecida)"

if [ "$(uname -m 2>/dev/null || true)" != "aarch64" ]; then
  printf '[info] O ambiente alvo é ARM64/aarch64. A arquitetura atual é diferente; confirme compatibilidade das imagens.\n'
fi

check_cmd 'git' git 'Instale git para gerenciar o histórico do repositório.'
check_cmd 'docker' docker 'Instale Docker Engine para a infraestrutura local.'

if command -v docker >/dev/null 2>&1; then
  if docker compose version >/dev/null 2>&1; then
    printf '[ok]   docker compose: %s\n' "$(docker compose version | head -n 1)"
  else
    printf '[aviso] plugin docker compose indisponível. Instale Docker Compose v2.\n'
    status=1
  fi
fi

check_cmd 'java' java 'Instale OpenJDK 21 antes do desenvolvimento da API.'
if command -v java >/dev/null 2>&1; then
  java -version 2>&1 | sed 's/^/[info] java: /'
fi

check_cmd 'mvn' mvn 'Instale Maven ou use o Maven wrapper quando a API existir.'
check_cmd 'node' node 'Node.js será necessário depois para a página institucional.'
check_cmd 'npm' npm 'npm será necessário depois para a página institucional.'

if [ ! -f .env ]; then
  printf '[info] .env não encontrado. Crie com: cp .env.example .env\n'
else
  printf '[ok]   .env encontrado\n'
fi

if [ -f docker-compose.yml ] && command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  if [ -f .env ]; then
    if docker compose --env-file .env config >/dev/null 2>&1; then
      printf '[ok]   docker compose config validou com .env\n'
    else
      printf '[aviso] docker compose config falhou com .env. Rode docker compose --env-file .env config para detalhes.\n'
      status=1
    fi
  else
    if docker compose --env-file .env.example config >/dev/null 2>&1; then
      printf '[ok]   docker compose config validou com .env.example\n'
    else
      printf '[aviso] docker compose config falhou com .env.example.\n'
      status=1
    fi
  fi
fi

if [ "$status" -eq 0 ]; then
  printf 'Diagnóstico concluído sem avisos críticos.\n'
else
  printf 'Diagnóstico concluído com avisos. O repositório ainda pode ser inspecionado, mas algumas fases dependem de instalações.\n'
fi

exit 0
