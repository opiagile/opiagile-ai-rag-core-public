#!/usr/bin/env bash
set -euo pipefail

tenant="${1:-demo}"
workspace="${2:-clinica-demo}"
name="${3:-Cliente API Demo}"
scopes="${4:-chat:write,documents:read,documents:upload,conversations:read,observability:read,handoffs:read,tools:read,workspaces:read}"
rate_limit="${5:-60}"

if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl não encontrado. Instale openssl antes de gerar a chave." >&2
  exit 1
fi

if ! command -v sha256sum >/dev/null 2>&1; then
  echo "sha256sum não encontrado. Instale coreutils antes de gerar a chave." >&2
  exit 1
fi

secret="$(openssl rand -base64 36 | tr -d '=+/' | cut -c1-40)"
api_key="opg_${secret}"
key_prefix="$(printf '%s' "$api_key" | cut -c1-12)"
key_hash="$(printf '%s' "$api_key" | sha256sum | awk '{print $1}')"

scope_array="$(printf '%s' "$scopes" | awk -F',' '{
  printf "ARRAY["
  for (i = 1; i <= NF; i++) {
    gsub(/^ +| +$/, "", $i)
    gsub(/\047/, "\047\047", $i)
    printf "%s'\''%s'\''", i == 1 ? "" : ", ", $i
  }
  printf "]::TEXT[]"
}')"
safe_name="$(printf '%s' "$name" | sed "s/'/''/g")"
safe_tenant="$(printf '%s' "$tenant" | sed "s/'/''/g")"
safe_workspace="$(printf '%s' "$workspace" | sed "s/'/''/g")"

cat <<EOF
Chave de API gerada. Copie agora; ela não deve ser commitada nem compartilhada.

API key:
$api_key

SQL para cadastrar no PostgreSQL:
INSERT INTO api_clients (
    tenant_id,
    workspace_id,
    name,
    key_prefix,
    key_hash,
    scopes,
    rate_limit_per_minute
)
SELECT t.id,
       w.id,
       '$safe_name',
       '$key_prefix',
       '$key_hash',
       $scope_array,
       $rate_limit
FROM tenants t
JOIN workspaces w ON w.tenant_id = t.id
WHERE t.slug = '$safe_tenant'
  AND w.slug = '$safe_workspace';
EOF
