# Piloto WhatsApp Cloud API

## Objetivo

Permitir teste controlado pelo WhatsApp real usando a API oficial da Meta, sem abrir uma demo pública irrestrita e sem tratar o projeto como produção.

O piloto conecta mensagens de texto recebidas pelo webhook da Meta ao `ChatService`, preservando RAG demonstrável com fontes, memória, triagem, lead, handoff e geração conversacional opcional por LLM.

## Modos Suportados

| Modo | Uso | Envia mensagem real? |
| --- | --- | --- |
| `MOCK` | Desenvolvimento local e testes sem credenciais | Não |
| `META_CLOUD` dry-run | Validar payload, assinatura, allowlist, rate limit e ChatService | Não |
| `META_CLOUD` envio real | Piloto com números autorizados | Sim, se todas as proteções estiverem válidas |

O padrão do `.env.example` é seguro: `WHATSAPP_PROVIDER=MOCK`, `WHATSAPP_SEND_ENABLED=false` e `WHATSAPP_DRY_RUN=true`.

## Variáveis De Ambiente

```text
WHATSAPP_PROVIDER=MOCK|META_CLOUD
WHATSAPP_VERIFY_TOKEN=
WHATSAPP_APP_SECRET=
WHATSAPP_ACCESS_TOKEN=
WHATSAPP_PHONE_NUMBER_ID=
WHATSAPP_BUSINESS_ACCOUNT_ID=
WHATSAPP_GRAPH_API_VERSION=v23.0
WHATSAPP_ALLOWED_TEST_NUMBERS=5511999999999,5511888888888
WHATSAPP_PUBLIC_BASE_URL=
WHATSAPP_SEND_ENABLED=false
WHATSAPP_DRY_RUN=true
WHATSAPP_SIGNATURE_REQUIRED=true
WHATSAPP_RATE_LIMIT_PER_MINUTE=5
WHATSAPP_BLOCK_UNAUTHORIZED=true
```

Nunca versionar o `.env` real. Nunca copiar tokens para README, issues, commits ou logs.

## Endpoints

```text
POST /api/webhooks/whatsapp          # mock local
GET  /api/webhooks/whatsapp/meta     # verificação da Meta
POST /api/webhooks/whatsapp/meta     # recebimento de eventos Meta
GET  /api/webhooks/whatsapp/status   # status operacional sem segredos
```

## Verificação Do Webhook

A Meta chama `GET /api/webhooks/whatsapp/meta` com `hub.mode`, `hub.verify_token` e `hub.challenge`.

O backend retorna o `hub.challenge` quando:

- `hub.mode=subscribe`;
- `hub.verify_token` bate com `WHATSAPP_VERIFY_TOKEN`;
- `hub.challenge` está presente.

Caso contrário, retorna HTTP 403.

## Assinatura

O endpoint `POST /api/webhooks/whatsapp/meta` valida o header `X-Hub-Signature-256` usando HMAC SHA-256 sobre o corpo bruto da requisição e `WHATSAPP_APP_SECRET`.

Regras:

- assinatura inválida bloqueia o POST quando `WHATSAPP_SIGNATURE_REQUIRED=true`;
- assinatura ausente bloqueia o POST quando requerida;
- `WHATSAPP_SIGNATURE_REQUIRED=false` só deve ser usado em ambiente local/dev e dry-run;
- app secret, token e assinatura completa nunca devem ser logados.

## Allowlist E Rate Limit

O piloto só processa números autorizados em `WHATSAPP_ALLOWED_TEST_NUMBERS`. Os números são normalizados antes da comparação, removendo espaços, `+`, parênteses e hífens.

O rate limit simples em memória limita mensagens por telefone por minuto. O padrão é `5`. Quando o limite é excedido, o backend não chama LLM e retorna 200 para evitar retentativas desnecessárias da Meta.

## Fluxo De Mensagem

1. Meta envia payload de webhook para HTTPS público.
2. Backend valida assinatura.
3. Parser extrai apenas mensagens de texto.
4. Eventos de status, mídia, áudio, imagem, documento, localização e botão são ignorados nesta sprint.
5. Backend valida allowlist e rate limit.
6. `ChatService` recebe `channel=WHATSAPP`, `contactId=telefone` e a mensagem.
7. Resposta é preparada para WhatsApp.
8. Em dry-run, nada é enviado para a Graph API.
9. Com envio real habilitado e número autorizado, o provider chama `POST https://graph.facebook.com/{version}/{phone-number-id}/messages`.

## Segurança Do Piloto

- Dry-run ativo por padrão.
- Envio real desabilitado por padrão.
- Allowlist obrigatória para envio real.
- Rate limit por telefone.
- Assinatura requerida por padrão.
- Logs com telefone mascarado.
- Endpoint de status sem segredos.
- Sem suporte a grupos, mídia, áudio ou templates nesta sprint.

## Primeiro Teste Real

Para o primeiro teste real, o tester deve iniciar a conversa pelo WhatsApp. Isso evita confusão com janela de atendimento e templates. Mensagens iniciadas pela empresa fora da janela de atendimento podem exigir templates aprovados pela Meta.

A ordem segura é:

1. `WHATSAPP_DRY_RUN=true` e `WHATSAPP_SEND_ENABLED=false`.
2. Validar recebimento real do webhook.
3. Confirmar assinatura válida.
4. Confirmar número na allowlist.
5. Confirmar resposta do `ChatService`.
6. Somente depois alterar para `WHATSAPP_DRY_RUN=false` e `WHATSAPP_SEND_ENABLED=true`, mantendo apenas o número do responsável na allowlist.

Checklist detalhado: [whatsapp-real-test-checklist.md](whatsapp-real-test-checklist.md).

## Requisitos Operacionais Para Teste Real

- App Meta configurado.
- WhatsApp Business Account.
- Phone Number ID.
- Access Token.
- App Secret.
- Verify Token.
- Endpoint HTTPS público temporário ou deploy controlado, sem commitar URL efêmera.
- Números de testers com consentimento e na allowlist. Começar apenas com o número do responsável.

## Limitações

Este piloto não é produção. Produção real exige revisão de segurança, LGPD, monitoramento, backup, política de retenção, suporte operacional, templates aprovados quando aplicável e plano de incidentes.
