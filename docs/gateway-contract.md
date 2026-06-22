# Contrato Para Gateways

Este documento define como aplicações externas devem consumir o `opiagile-ai-rag-core`.

O objetivo é manter este repositório focado em RAG, fontes, memória, resposta com LLM opcional, observabilidade e avaliação. Canais como WhatsApp, Teams, webchat público ou CRM devem ficar em aplicações próprias, por exemplo `opiagile-whatsapp-ai-gateway`.

## Princípios

- O gateway não acessa diretamente o banco do core.
- O gateway não implementa RAG.
- O gateway transforma mensagens do canal em chamadas HTTP para o core.
- O core responde com texto, fontes, metadados de conversa e sinais operacionais.
- Segredos do canal ficam no gateway.
- Segredos de LLM e embeddings ficam no core.
- Dados sensíveis devem ser minimizados antes de atravessar o contrato.

## Endpoint Principal

```text
POST /api/chat
```

Payload:

```json
{
  "conversationId": "opcional",
  "message": "string",
  "channel": "WHATSAPP",
  "contactId": "5511999999999"
}
```

Resposta:

```json
{
  "conversationId": "uuid",
  "answer": "resposta para o usuário final",
  "intent": "DUVIDA_FAQ",
  "sources": [
    {
      "documentId": "uuid",
      "filename": "faq.txt",
      "chunkId": "uuid",
      "score": 0.91,
      "excerpt": "trecho usado como fonte"
    }
  ],
  "handoffRequired": false,
  "leadStatus": "QUALIFYING",
  "latencyMs": 123,
  "responseMode": "DEMO ou LLM",
  "llmProvider": "DEMO ou OPENAI",
  "model": "gpt-5-mini",
  "fallbackReason": null,
  "llmLatencyMs": 110,
  "promptTokens": 100,
  "completionTokens": 80,
  "totalTokens": 180
}
```

## Observabilidade

```text
GET /api/observability/conversations/{conversationId}/trace
```

Uso recomendado pelo gateway:

- consultar trace apenas para debug operacional;
- não expor trace bruto ao usuário final;
- não persistir fontes completas se o canal não precisar;
- nunca registrar tokens, chaves, app secret ou credenciais do canal.

## Handoff

```text
GET /api/handoffs
GET /api/handoffs/{id}
PATCH /api/handoffs/{id}/status
```

O gateway deve tratar `handoffRequired=true` como sinal para:

- responder de forma acolhedora ao usuário;
- notificar fila humana, CRM ou painel externo;
- não insistir em automação quando a intenção exigir pessoa humana.

## Documentos

```text
POST /api/documents/upload
GET /api/documents
GET /api/documents/{id}
GET /api/documents/{id}/chunks
```

Esses endpoints pertencem ao core. Gateways de canal normalmente não precisam usá-los, exceto em ferramentas internas de administração.

## Canais Recomendados

Valores sugeridos para `channel`:

- `WEB`
- `WHATSAPP`
- `TEAMS`
- `SLACK`
- `CRM`
- `API`

## Evolução Do Contrato

Campos novos devem ser adicionados de forma compatível. Aplicações gateway não devem depender de detalhes internos como:

- nomes de tabelas;
- formato de prompt;
- estratégia de chunking;
- implementação de embeddings;
- tipo de índice vetorial;
- provider de LLM.

## Aplicação Gateway Planejada

Nome sugerido:

```text
opiagile-whatsapp-ai-gateway
```

Responsabilidades:

- receber webhooks da Meta;
- validar assinatura e allowlist;
- aplicar rate limit do canal;
- chamar `POST /api/chat`;
- enviar resposta pelo WhatsApp Cloud API;
- manter segredos Meta fora do core;
- registrar logs operacionais do canal sem expor credenciais.
