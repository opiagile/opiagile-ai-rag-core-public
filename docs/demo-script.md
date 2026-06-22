# Roteiro De Demonstração


## Posicionamento

Frase padrão da demonstração:

> Core RAG com fontes, fallback textual local, pgvector com embeddings reais quando configurado e modo local sem chave externa.

Na v0.4, quando OpenAI está configurada, a resposta fica conversacional por LLM; sem chave externa, o fallback local continua funcionando.

Esta versão é um portfólio técnico publicado e validado por E2E local. Demo pública aberta e produção real ainda são próximos incrementos. O WhatsApp Cloud API está preservado como piloto controlado de referência para futura extração em gateway.

## Preparação

```bash
cp .env.example .env
docker compose --env-file .env up -d postgres
cd backend
./mvnw spring-boot:run
```


## Roteiro Prático Por API

Suba a API e use os exemplos abaixo com Python, curl ou cliente HTTP. A antiga Demo UI foi removida deste core. O contrato para recriar o frontend está em [`frontend-handoff.md`](frontend-handoff.md).

## Passo 1: Ingerir Documento

Enviar `samples/clinica/faq.txt` para `POST /api/documents/upload`.

Resultado esperado: documento `INDEXED`, chunks persistidos e provider `noop` no modo local ou `openai` quando embeddings reais estiverem ativos.

## Passo 2: Perguntar Com RAG

Pergunta: `Vocês atendem aos sábados?`

Resultado esperado: resposta com horário de sábado, fontes, `responseMode`, provider e `handoffRequired=false`.

## Passo 3: Memória

Primeira mensagem: `Qual o horário de atendimento?`

Segunda mensagem com mesmo `conversationId`: `E aos sábados?`

Resultado esperado: segunda resposta usa contexto da primeira.

## Passo 4: Lead

Mensagem: `Meu nome é Maria, telefone (11) 99999-7777, email maria@exemplo.com. Quero agendar consulta.`

Resultado esperado: intenção `AGENDAR`, lead `QUALIFIED` e dados salvos.

## Passo 5: Handoff

Mensagem: `Quero falar com um atendente humano.`

Resultado esperado: `handoffRequired=true`, lead `NEEDS_HUMAN` e registro em `/api/handoffs`.

## Passo 6: Observabilidade

Consultar `GET /api/observability/conversations/{id}/trace`.

Resultado esperado: mensagens, retrievals, provider, modo de resposta, modelo, fallback e handoffs.


## Passo 7: Webhook Estilo WhatsApp Mock

Enviar payload simulado para `POST /api/webhooks/whatsapp`.

```json
{
  "provider": "MOCK",
  "from": "+5511999999999",
  "name": "João",
  "message": "Quero agendar uma consulta",
  "timestamp": "2026-05-28T10:00:00Z"
}
```

Resultado esperado: conversa criada/atualizada por telefone, resposta simulada e intenção detectada.


## Passo 8: Piloto WhatsApp Cloud API

Consultar `GET /api/webhooks/whatsapp/status`.

Resultado esperado: status operacional do provider, dry-run, envio habilitado, assinatura requerida, campos configurados, quantidade de números autorizados e rate limit. Nenhum token, app secret, verify token ou número completo deve aparecer.

Para validação com a Meta, use `GET /api/webhooks/whatsapp/meta` para verificação do webhook e `POST /api/webhooks/whatsapp/meta` com payload simulado. Envio real só deve ser habilitado com HTTPS público, credenciais seguras, assinatura válida, allowlist e consentimento dos testers.
