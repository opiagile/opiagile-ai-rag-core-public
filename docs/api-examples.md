# Exemplos De API

## Workspaces

```bash
curl http://localhost:8080/api/workspaces
```

Use os headers abaixo para isolar upload, listagem e chat por tenant/workspace:

```text
X-Tenant-Id: demo
X-Workspace-Id: clinica-demo
```

## Upload TXT

```bash
python3 - <<'PY'
import pathlib, urllib.request, uuid
boundary = '----opiagile' + uuid.uuid4().hex
path = pathlib.Path('samples/clinica/faq.txt')
body = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{path.name}"\r\nContent-Type: text/plain\r\n\r\n').encode() + path.read_bytes() + f'\r\n--{boundary}--\r\n'.encode()
req = urllib.request.Request(
    'http://localhost:8080/api/documents/upload',
    data=body,
    headers={
        'Content-Type': f'multipart/form-data; boundary={boundary}',
        'X-Tenant-Id': 'demo',
        'X-Workspace-Id': 'clinica-demo',
    },
    method='POST')
print(urllib.request.urlopen(req).read().decode())
PY
```

## Chat

```json
{
  "message": "Vocês atendem aos sábados?",
  "channel": "WEB",
  "contactId": "demo"
}
```

## Webhook WhatsApp

```json
{
  "provider": "MOCK",
  "from": "+5511999999999",
  "name": "João",
  "message": "Quero agendar uma consulta",
  "timestamp": "2026-05-28T08:53:00-04:00"
}
```

## Alterar Status De Handoff

```json
{
  "status": "IN_PROGRESS"
}
```

## Campos De Resposta Conversacional v0.4

`POST /api/chat` mantém os campos anteriores e pode retornar metadados adicionais:

```json
{
  "responseMode": "DEMO",
  "llmProvider": "DEMO",
  "model": "local-deterministico",
  "fallbackReason": null,
  "llmLatencyMs": 0,
  "promptTokens": null,
  "completionTokens": null,
  "totalTokens": null
}
```

Com `CHAT_RESPONSE_MODE=LLM`, `LLM_PROVIDER=OPENAI` e `OPENAI_API_KEY`, `responseMode` deve retornar `LLM` quando a resposta for gerada pela OpenAI. Sem chave ou em caso de erro, o backend usa fallback local e preenche `fallbackReason`.


## WhatsApp Cloud API Piloto

### Status Operacional Sem Segredos

```bash
curl http://localhost:8080/api/webhooks/whatsapp/status
```

Resposta esperada: provider, dry-run, envio habilitado, assinatura requerida, campos configurados, quantidade de números autorizados e rate limit. O endpoint não retorna tokens, app secret, verify token nem a lista completa de telefones.

### Verificação Do Webhook Meta

Use um token fictício local configurado em `WHATSAPP_VERIFY_TOKEN` no `.env`. Não use token real em documentação.

```bash
curl "http://localhost:8080/api/webhooks/whatsapp/meta?hub.mode=subscribe&hub.verify_token=TOKEN_DEMO&hub.challenge=12345"
```

Com token correto, o backend retorna `12345`. Com token incorreto, retorna HTTP 403.

### Payload Inbound Meta Simulado

Payload fictício de mensagem texto. Em ambiente real com `WHATSAPP_SIGNATURE_REQUIRED=true`, a Meta envia o header `X-Hub-Signature-256`; em testes automatizados a assinatura é validada com HMAC SHA-256.

```json
{
  "object": "whatsapp_business_account",
  "entry": [
    {
      "id": "123456789",
      "changes": [
        {
          "field": "messages",
          "value": {
            "metadata": {
              "display_phone_number": "15551234567",
              "phone_number_id": "12345"
            },
            "contacts": [
              {
                "profile": { "name": "João" },
                "wa_id": "5511999998888"
              }
            ],
            "messages": [
              {
                "from": "5511999998888",
                "id": "wamid.TESTE",
                "timestamp": "1760000000",
                "type": "text",
                "text": { "body": "Quero agendar uma consulta" }
              }
            ]
          }
        }
      ]
    }
  ]
}
```

### Dry-run

Com `WHATSAPP_PROVIDER=META_CLOUD`, `WHATSAPP_DRY_RUN=true`, número na allowlist e assinatura válida ou desabilitada somente em ambiente local, o backend processa a mensagem, chama o `ChatService` e registra que a resposta seria enviada, mas não chama a Graph API.

Envio real exige `WHATSAPP_SEND_ENABLED=true`, `WHATSAPP_DRY_RUN=false`, token, Phone Number ID, número autorizado e HTTPS público configurado no painel da Meta.


Checklist operacional do primeiro teste real: [`docs/whatsapp-real-test-checklist.md`](whatsapp-real-test-checklist.md).
