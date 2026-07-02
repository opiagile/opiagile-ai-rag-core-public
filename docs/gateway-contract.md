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
- O gateway deve autenticar no core com API key tenant-aware, não com headers livres de tenant/workspace em ambientes expostos.

## Autenticação Do Gateway

Gateways devem enviar a chave no header:

```text
X-OPIAGILE-API-KEY: <api-key-do-gateway>
```

Também é aceito:

```text
Authorization: Bearer <api-key-do-gateway>
```

A chave real nunca é armazenada em texto puro no banco. A tabela `api_clients` guarda apenas `key_hash` com SHA-256, `key_prefix` para identificação operacional, tenant/workspace vinculado, status, escopos e rate limit.

Em ambiente publicado, configure:

```text
API_SECURITY_ENABLED=true
API_SECURITY_REQUIRE_API_KEY=true
```

O modo legado com `X-Tenant-Id` e `X-Workspace-Id` deve ficar restrito a desenvolvimento local ou migração controlada.

Escopos recomendados por tipo de gateway:

| Gateway | Escopos mínimos |
| --- | --- |
| Webchat público | `chat:write` |
| Painel de documentos | `documents:read`, `documents:upload` |
| Painel operacional | `conversations:read`, `observability:read`, `handoffs:read`, `handoffs:write` |
| Ferramentas internas | `tools:read`, `tools:execute` |
| Seletor de workspace | `workspaces:read` |

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
  "contactId": "5511999999999",
  "responseLanguage": "PORTUGUESE"
}
```

`responseLanguage` é opcional. Valores aceitos:

- `ENGLISH`
- `SPANISH`
- `PORTUGUESE`

Quando omitido, o core usa `PORTUGUESE` para manter compatibilidade com gateways existentes. Frontends internacionais devem enviar esse campo de acordo com o idioma selecionado pelo usuário.

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
- em ambientes de demonstração protegidos, enviar `X-Demo-Token` ou `Authorization: Bearer` nas chamadas de escrita;
- tratar `429 LIMITE_DEMO_EXCEDIDO` como sinal para reduzir frequência de chamadas.

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

## Tenant E Workspace

Em ambiente local ou legado, gateways e frontends podem enviar o escopo de conhecimento em chamadas que leem ou escrevem dados de RAG:

```text
X-Tenant-Id: demo
X-Workspace-Id: clinica-demo
```

Sem headers, o core usa o padrão configurado por:

```text
TENANT_DEFAULT_TENANT=demo
TENANT_DEFAULT_WORKSPACE=clinica-demo
```

Workspaces demo disponíveis:

- `clinica-demo`: base de clínica/consultório;
- `atendimento-demo`: base de atendimento geral e handoff;
- `locacao-demo`: base de locação imobiliária.

O backend filtra documentos e chunks por tenant/workspace no upload, listagem, busca textual, busca pgvector e chat. Um gateway não deve permitir que o usuário final altere tenant/workspace livremente sem autorização.

Com API key tenant-aware, o core ignora esses headers e usa o tenant/workspace vinculado à credencial. Esse é o modelo recomendado para separar clientes, departamentos ou aplicações.

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
