# Exemplos De API

## Portal De Desenvolvedores E OpenAPI

Portal visual para devs:

```bash
curl http://localhost:8080/developers/
```

Console Opiagile com navegação lateral:

```text
http://localhost:8080/developers/console
http://localhost:8080/developers/api-console/index.html
```

Swagger UI clássico, preservado como fallback técnico:

```text
http://localhost:8080/developers/swagger-ui/index.html
```

Especificação OpenAPI do core:

```bash
curl http://localhost:8080/v3/api-docs/rag-core
```

No console Opiagile, informe sua API key no campo `X-OPIAGILE-API-KEY`. O console permite consultar `GET /api/workspaces` para exibir tenant/workspace permitidos pela chave.

No Swagger clássico, use `Authorize` e informe sua API key no esquema `OpiagileApiKey`. A chave é enviada no header:

```text
X-OPIAGILE-API-KEY: SUA_API_KEY_TENANT_AWARE
```

Não use API key real em prints, documentação pública, código frontend ou repositórios.

## Workspaces

```bash
curl http://localhost:8080/api/workspaces
```

Em desenvolvimento local legado, os headers abaixo isolam upload, listagem e chat por tenant/workspace:

```text
X-Tenant-Id: demo
X-Workspace-Id: clinica-demo
```

Em ambientes expostos, prefira API key tenant-aware. Com uma chave válida, o backend usa o tenant/workspace vinculado à credencial, ignora headers livres e permite consultar os workspaces disponíveis:

```bash
curl http://localhost:8080/api/workspaces \
  -H "X-OPIAGILE-API-KEY: $OPIAGILE_CORE_API_KEY"
```

Gere uma chave operacional com:

```bash
scripts/generate-api-client-key.sh demo clinica-demo "Gateway Demo" "chat:write,documents:read,documents:upload,conversations:read,observability:read,providers:read,workspaces:read" 60
```

O script mostra a chave uma única vez e gera o SQL de cadastro em `api_clients` com hash SHA-256.

## Administração De Solicitações Developer

Os endpoints administrativos abaixo não devem ser tratados como fluxo público. Use apenas em ambiente controlado, com `X-Demo-Admin-Token`, preferencialmente via SSH/túnel ou acesso operacional restrito.

Listar solicitações pendentes:

```bash
curl http://localhost:8080/api/admin/developer-access-requests \
  -H "X-Demo-Admin-Token: <admin-token>"
```

Aprovar uma solicitação criando tenant/workspace temporário para o lead:

```bash
curl -X POST http://localhost:8080/api/admin/developer-access-requests/<request-id>/approve-temporary-sandbox \
  -H "Content-Type: application/json" \
  -H "X-Demo-Admin-Token: <admin-token>" \
  -d '{
    "scopes": ["chat:write", "documents:upload", "documents:read", "providers:read", "workspaces:read"],
    "rateLimitPerMinute": 30,
    "clientName": "Sandbox - Empresa Demo",
    "expiresInHours": 24
  }'
```

A resposta retorna `apiKey`, `tenantSlug`, `workspaceSlug`, `expiresAt`, `retentionNotice`, `keyDeliveryUrl` e `keyDeliveryExpiresAt`.

O endpoint administrativo ainda exibe a API key uma única vez para contingência operacional, mas o fluxo recomendado é a entrega automática por email: o core cria um link de uso único e coloca emails na fila assíncrona para o lead e para `contato@opiagile.com`. O link permite revelar a chave uma única vez e expira conforme `DEVELOPER_ACCESS_KEY_DELIVERY_EXPIRES_IN_HOURS`.

Após expirar, o tenant/workspace e dados enviados para teste são excluídos do sandbox; a solicitação do lead permanece registrada para contato e auditoria operacional, conforme práticas de LGPD.

Aprovar uma solicitação usando workspace existente:

```bash
curl -X POST http://localhost:8080/api/admin/developer-access-requests/<request-id>/approve \
  -H "Content-Type: application/json" \
  -H "X-Demo-Admin-Token: <admin-token>" \
  -d '{
    "tenantSlug": "demo",
    "workspaceSlug": "clinica-demo",
    "scopes": ["chat:write", "documents:upload", "documents:read", "providers:read", "workspaces:read"],
    "rateLimitPerMinute": 30,
    "clientName": "Sandbox - Empresa Demo"
  }'
```

A resposta retorna `apiKey` uma única vez e também `keyDeliveryUrl` para entrega ao lead. O core grava apenas prefixo, hash da chave e uma cópia criptografada temporária para o link de uso único.

Quando SMTP estiver ativo, não é necessário copiar a chave manualmente para o lead: o scheduler envia o link para o email informado na solicitação.

## Entrega De Chave Por Link De Uso Único

O link gerado na aprovação segue o formato:

```text
https://opiagile.com/developers/access-key/<token>
```

Fluxo esperado:

1. O lead abre o link recebido por email.
2. A página informa que a API key só será revelada uma vez.
3. O lead clica em `Revelar API key uma única vez`.
4. A chave, tenant, workspace, escopos, limite por minuto e expiração aparecem na tela.
5. O link é consumido e não exibe a chave novamente.

Não inclua tokens reais de entrega em documentação, prints ou issues.

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
        'X-OPIAGILE-API-KEY': 'SUA_API_KEY_TENANT_AWARE',
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
  "contactId": "demo",
  "responseLanguage": "PORTUGUESE"
}
```

`responseLanguage` é opcional e aceita `ENGLISH`, `SPANISH` ou `PORTUGUESE`. Use esse campo para forçar a resposta da LLM/fallback no mesmo idioma selecionado pela interface.

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

Para testar a POC de embeddings com Spring AI, configure:

```text
OPENAI_EMBEDDINGS_ENABLED=true
OPENAI_EMBEDDINGS_PROVIDER=spring-ai
SPRING_AI_MODEL_EMBEDDING=openai
OPENAI_API_KEY=<valor fora do Git>
```

Se `SPRING_AI_MODEL_EMBEDDING` não estiver configurado, o core preserva fallback textual e não derruba a aplicação.

## Status Seguro De Provedores

```bash
curl http://localhost:8080/api/providers/status
```

Resposta esperada em modo local sem chave:

```json
{
  "status": "ATENCAO",
  "chat": {
    "requestedResponseMode": "DEMO",
    "requestedProvider": "DEMO",
    "activeProvider": "DEMO",
    "model": "local-deterministico",
    "openAiApiKeyConfigured": false,
    "fallbackEnabled": true,
    "fallbackProvider": "DEMO",
    "status": "OPERACIONAL"
  },
  "embeddings": {
    "enabled": false,
    "activeProvider": "NOOP",
    "model": "text-embedding-3-small",
    "dimensions": 1536,
    "openAiApiKeyConfigured": false,
    "fallbackEnabled": true,
    "fallbackProvider": "LOCAL_TEXT",
    "status": "DESABILITADO_FALLBACK_TEXTUAL"
  },
  "retrieval": {
    "activeStrategy": "LOCAL_TEXT",
    "pgvectorReadyByConfiguration": false,
    "fallbackEnabled": true,
    "fallbackProvider": "LOCAL_TEXT",
    "status": "FALLBACK_TEXTUAL"
  },
  "toolPlanner": {
    "enabled": true,
    "activeProvider": "NONE",
    "openAiApiKeyConfigured": false,
    "status": "INATIVO_POR_CHAVE_AUSENTE"
  },
  "warnings": [
    "OPENAI_EMBEDDINGS_DESABILITADO; recuperação usa texto local até habilitar embeddings.",
    "TOOLS_PLANNER_LLM_SEM_CHAVE; planner LLM fica inativo."
  ]
}
```

O endpoint não retorna chaves, tokens ou valores de secrets. Ele apenas informa booleans e modos operacionais.

## Ferramentas Controladas

Liste as ferramentas disponíveis no tenant/workspace:

```bash
curl http://localhost:8080/api/tools \
  -H "X-Tenant-Id: opiagile" \
  -H "X-Workspace-Id: opiagile-rag"
```

Execute consulta SQL somente leitura em ferramenta autorizada:

```bash
curl -X POST http://localhost:8080/api/tools/base-conhecimento-readonly/sql/query \
  -H "Content-Type: application/json" \
  -H "X-OPIAGILE-API-KEY: $OPIAGILE_CORE_API_KEY" \
  -d '{
    "sql": "select filename, status, created_at from documents order by created_at desc",
    "maxRows": 10
  }'
```

Por padrão, execução de tools exige token administrativo legado ou API key com escopo `tools:execute`. A consulta aceita apenas `SELECT`, usa allowlist de tabelas e aplica limite de linhas. Não inclua segredos, tokens ou dados sensíveis nos exemplos.

## Auditoria De API Clients

Consulta administrativa de uso das API keys:

```bash
curl http://localhost:8080/api/admin/api-clients/usage \
  -H "X-Demo-Admin-Token: TOKEN_ADMIN_LOCAL"
```

Resposta: resumo por client e eventos recentes com endpoint, escopo, status, bloqueio e latência. O endpoint não retorna API keys nem payloads de mensagens.

### Uso Automático Pelo Chat

O `POST /api/chat` pode acionar ferramentas controladas em perguntas operacionais simples sobre a base do workspace. Exemplo:

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -H "X-OPIAGILE-API-KEY: $OPIAGILE_CORE_API_KEY" \
  -d '{
    "message": "Quantos documentos existem nesta base?",
    "channel": "WEB",
    "contactId": "teste-tools"
  }'
```

O backend decide se a pergunta combina com uma consulta pré-aprovada, executa a ferramenta read-only e injeta o resultado no contexto da resposta. O usuário final não envia SQL pelo chat.

Com `TOOLS_PLANNER_LLM_ENABLED=true` e `OPENAI_API_KEY` configurada, a LLM pode ajudar a classificar perguntas operacionais menos literais. Ela não gera SQL: retorna apenas uma ação permitida, e o backend monta a consulta segura.


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
