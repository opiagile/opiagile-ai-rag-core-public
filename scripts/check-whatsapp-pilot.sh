#!/usr/bin/env bash
set -u

load_env_file() {
  if [ ! -f .env ]; then
    printf '[info] .env não encontrado. O diagnóstico usará apenas variáveis já exportadas.\n'
    return
  fi
  while IFS='=' read -r key value; do
    case "$key" in
      ''|'#'*) continue ;;
    esac
    if ! printf '%s' "$key" | grep -Eq '^[A-Za-z_][A-Za-z0-9_]*$'; then
      continue
    fi
    if [ -z "${!key-}" ]; then
      value="${value%%#*}"
      value="$(printf '%s' "$value" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//' -e 's/^"//' -e 's/"$//' -e "s/^'//" -e "s/'$//")"
      export "$key=$value"
    fi
  done < .env
  printf '[info] .env carregado com parser seguro; segredos não serão impressos.\n'
}

mask_phone() {
  local raw="$1"
  local digits
  digits="$(printf '%s' "$raw" | tr -cd '0-9')"
  local len=${#digits}
  if [ "$len" -le 4 ]; then
    printf '****'
  elif [ "$len" -le 8 ]; then
    printf '%s****%s' "${digits:0:2}" "${digits: -2}"
  else
    printf '%s****%s' "${digits:0:4}" "${digits: -4}"
  fi
}

has_value() {
  local name="$1"
  local value="${!name-}"
  if [ -n "$value" ]; then
    printf '[ok]   %s presente
' "$name"
  else
    printf '[aviso] %s ausente
' "$name"
  fi
}

print_value() {
  local name="$1"
  local value="${!name-}"
  if [ -n "$value" ]; then
    printf '[info] %s=%s
' "$name" "$value"
  else
    printf '[info] %s=<ausente>
' "$name"
  fi
}

count_allowlist() {
  local raw="${WHATSAPP_ALLOWED_TEST_NUMBERS-}"
  if [ -z "$raw" ]; then
    printf '[aviso] WHATSAPP_ALLOWED_TEST_NUMBERS ausente; envio real deve ficar bloqueado.
'
    return
  fi
  IFS=',' read -r -a numbers <<< "$raw"
  local count=0
  local masked=()
  for number in "${numbers[@]}"; do
    local trimmed
    trimmed="$(printf '%s' "$number" | xargs 2>/dev/null || printf '%s' "$number")"
    if [ -n "$trimmed" ]; then
      count=$((count + 1))
      masked+=("$(mask_phone "$trimmed")")
    fi
  done
  printf '[ok]   WHATSAPP_ALLOWED_TEST_NUMBERS contém %s número(s): %s
' "$count" "${masked[*]}"
}

printf 'Diagnóstico seguro do piloto WhatsApp Cloud API
'
printf 'Este script não imprime tokens, secrets ou telefones completos.

'
load_env_file
printf '
'

print_value WHATSAPP_PROVIDER
print_value WHATSAPP_DRY_RUN
print_value WHATSAPP_SEND_ENABLED
print_value WHATSAPP_SIGNATURE_REQUIRED
print_value WHATSAPP_RATE_LIMIT_PER_MINUTE

has_value WHATSAPP_VERIFY_TOKEN
has_value WHATSAPP_APP_SECRET
has_value WHATSAPP_ACCESS_TOKEN
has_value WHATSAPP_PHONE_NUMBER_ID
has_value WHATSAPP_BUSINESS_ACCOUNT_ID

if [ -n "${WHATSAPP_PUBLIC_BASE_URL-}" ]; then
  printf '[ok]   WHATSAPP_PUBLIC_BASE_URL configurada
'
else
  printf '[aviso] WHATSAPP_PUBLIC_BASE_URL ausente; a Meta precisa de HTTPS público para chamar o webhook.
'
fi

count_allowlist

if [ "${WHATSAPP_PROVIDER-}" = "META_CLOUD" ] && [ "${WHATSAPP_SEND_ENABLED-}" = "true" ] && [ "${WHATSAPP_DRY_RUN-}" != "false" ]; then
  printf '[aviso] Envio habilitado, mas dry-run ainda está ativo. Nenhuma mensagem real deve ser enviada.
'
fi

if [ "${WHATSAPP_PROVIDER-}" = "META_CLOUD" ] && [ "${WHATSAPP_SEND_ENABLED-}" = "true" ] && [ "${WHATSAPP_DRY_RUN-}" = "false" ] && [ -z "${WHATSAPP_ALLOWED_TEST_NUMBERS-}" ]; then
  printf '[erro] Envio real sem allowlist. Mantenha o envio bloqueado.
'
  exit 1
fi

printf '
Diagnóstico concluído. Confirme dry-run antes do primeiro teste real.
'
