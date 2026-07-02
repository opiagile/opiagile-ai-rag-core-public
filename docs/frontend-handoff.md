# Handoff Para Frontend Separado

Este documento registra o estado da antiga `demo-ui/` antes da remoção do frontend deste repositório. O objetivo é permitir que outro agente ou outro projeto crie uma interface web separada consumindo o `opiagile-ai-rag-core`.

## Decisão De Arquitetura

O repositório `opiagile-ai-rag-core` deve permanecer focado no backend de RAG:

- ingestão de documentos;
- chunking;
- embeddings opcionais;
- recuperação textual ou pgvector;
- geração conversacional com LLM opcional;
- fontes;
- memória conversacional;
- intenção, lead, handoff e observabilidade;
- contrato HTTP para clientes externos.

Interfaces visuais devem viver em outro repositório, por exemplo:

```text
opiagile-rag-demo-web
```

Gateways de canal devem viver em aplicações próprias, por exemplo:

```text
opiagile-whatsapp-ai-gateway
```

## Escopo Do Novo Frontend

A interface deve demonstrar o core para usuário leigo com fluxo claro:

1. verificar se a API está online;
2. escolher o workspace demo;
3. enviar um arquivo `.txt` para o workspace selecionado;
4. listar documentos indexados do workspace;
5. fazer pergunta no chat;
6. ver resposta em formato de conversa;
7. ver fontes usadas;
8. ver modo de resposta (`DEMO` ou `LLM`);
9. ver intenção, lead status e handoff;
10. consultar trace básico da conversa.

O frontend não deve armazenar segredos, tokens ou chaves de provedor.

## Escopo Multi-Tenant

O core usa headers para isolar documentos, conversas e retrieval:

```text
X-Tenant-Id: demo
X-Workspace-Id: clinica-demo
```

Workspaces demo:

| Workspace | Tela sugerida | Sample sugerido |
| --- | --- | --- |
| `clinica-demo` | Clínica Demo | `samples/clinica/faq.txt` |
| `atendimento-demo` | Atendimento Demo | `samples/atendimento/faq.txt` |
| `locacao-demo` | Locação Demo | `samples/imobiliaria/faq.txt` |

O frontend deve aplicar esses headers em:

- `POST /api/documents/upload`;
- `GET /api/documents`;
- `GET /api/documents/{id}`;
- `GET /api/documents/{id}/chunks`;
- `POST /api/chat`.

Também deve consumir `GET /api/workspaces` para montar as telas/abas disponíveis.

## Base URL

Em desenvolvimento local, use proxy do Vite ou equivalente:

```text
/api      -> http://localhost:8080/api
/actuator -> http://localhost:8080/actuator
```

Em ambiente publicado, configure a base via variável de ambiente, por exemplo:

```text
VITE_API_BASE_URL=https://api.exemplo.com/api
VITE_ACTUATOR_BASE_URL=https://api.exemplo.com/actuator
```

## Endpoints Consumidos

### Status

```http
GET /actuator/health
GET /api/version
```

Uso esperado:

- mostrar API online/offline;
- mostrar nome, versão, ambiente, Java e timestamp;
- exibir erro amigável se a API estiver indisponível.

### Documentos

```http
POST /api/documents/upload
GET  /api/documents
GET  /api/documents/{documentId}
GET  /api/documents/{documentId}/chunks
```

Upload deve usar `multipart/form-data` com campo `file`.

Resposta típica de upload:

```json
{
  "documentId": "uuid",
  "filename": "faq.txt",
  "status": "INDEXED",
  "chunkCount": 2,
  "embeddingProvider": "openai"
}
```

Observações:

- usar `documentId` como identificador principal;
- aceitar apenas `.txt` na primeira versão;
- avisar que arquivos devem ser fictícios ou aprovados;
- não permitir envio de dados sensíveis em demonstração pública.

### Chat RAG

```http
POST /api/chat
```

Payload:

```json
{
  "conversationId": "opcional",
  "message": "Vocês atendem aos sábados?",
  "channel": "WEB",
  "contactId": "demo-web",
  "responseLanguage": "PORTUGUESE"
}
```

Para interfaces multilíngues, enviar `responseLanguage` conforme o idioma selecionado:

| Idioma da UI | Valor enviado |
| --- | --- |
| Inglês | `ENGLISH` |
| Espanhol | `SPANISH` |
| Português | `PORTUGUESE` |

Se o campo for omitido, o core responde em português por compatibilidade.

Resposta esperada:

```json
{
  "conversationId": "uuid",
  "answer": "Sim. Atendemos aos sábados das 8h às 12h...",
  "intent": "DUVIDA_FAQ",
  "leadStatus": "QUALIFYING",
  "handoffRequired": false,
  "sources": [
    {
      "documentId": "uuid",
      "filename": "faq.txt",
      "chunkId": "uuid",
      "score": 0.84,
      "excerpt": "Trecho recuperado..."
    }
  ],
  "latencyMs": 721,
  "responseMode": "LLM",
  "llmProvider": "OPENAI",
  "model": "gpt-5-mini",
  "fallbackReason": null
}
```

Comportamento de UI:

- preencher automaticamente o `conversationId` retornado;
- reutilizar o `conversationId` nas próximas mensagens;
- mostrar resposta em balão de conversa;
- mostrar fontes em painel lateral;
- destacar `handoffRequired=true`;
- mostrar badge de modo `DEMO` ou `LLM`;
- quando `fallbackReason` existir, exibir aviso de fallback.

### Conversas

```http
GET /api/conversations/{conversationId}/messages
GET /api/conversations/{conversationId}/summary
```

Uso esperado:

- reconstruir histórico recente;
- mostrar resumo operacional quando existir;
- ajudar usuário a entender contexto mantido pela API.

### Handoff

```http
GET   /api/handoffs
GET   /api/handoffs/{id}
PATCH /api/handoffs/{id}/status
```

Uso esperado:

- listar solicitações abertas;
- mostrar motivo, resumo, status e conversa;
- permitir atualização de status apenas se houver necessidade no produto visual.

### Observabilidade

```http
GET /api/observability/conversations/{conversationId}/trace
```

Uso esperado:

- mostrar mensagens;
- mostrar recuperações RAG;
- mostrar provider de recuperação;
- mostrar modo de resposta;
- mostrar modelo, fallback e fontes;
- renderizar JSON formatado como fallback quando o contrato mudar.

### Status De Canal Legado

O core ainda preserva endpoints de WhatsApp enquanto o módulo não é extraído para gateway separado:

```http
GET /api/webhooks/whatsapp/status
```

Para um frontend de RAG puro, esse endpoint pode ser ignorado. Se exibido, nunca mostrar tokens, app secret, verify token ou telefones completos.

## Estados Visuais Recomendados

- API offline;
- API online;
- banco sem documentos;
- upload em andamento;
- upload concluído;
- upload com erro;
- chat sem documento indexado;
- pergunta em andamento;
- resposta com fontes;
- resposta sem fontes;
- modo `DEMO`;
- modo `LLM`;
- fallback local;
- handoff requerido;
- trace indisponível.

## UX Recomendada

A tela principal deve parecer uma experiência de atendimento, não um painel técnico:

- header com nome do produto e status da API;
- coluna central com chat em balões;
- painel lateral com fontes, intenção, lead status, modo e trace;
- área de documentos simples para upload e listagem;
- mensagens em português;
- avisos claros sobre dados fictícios;
- metadados técnicos como apoio, não como conteúdo principal.

Textos obrigatórios em ambiente de demonstração:

```text
Demo local de portfólio técnico. Não é ambiente produtivo nem demo pública aberta.
```

```text
Use dados fictícios ou previamente aprovados. Não envie dados sensíveis.
```

## Exemplos De Perguntas

Use `samples/clinica/faq.txt` para uma primeira demonstração:

- `Vocês atendem aos sábados?`
- `Quais documentos preciso levar?`
- `Meu nome é João e quero agendar uma consulta.`
- `E aos sábados?`
- `Quero falar com um atendente humano.`
- `Vocês fazem cirurgia de emergência?`

## Validação Contra A VPS

Ambiente atual de validação:

```text
http://<IP_PUBLICO_DA_VPS>
```

Testes mínimos:

1. `GET /actuator/health` retorna `UP`;
2. upload de `samples/clinica/faq.txt` retorna `status=INDEXED`;
3. `POST /api/chat` retorna `answer`, `conversationId` e `sources`;
4. com OpenAI configurado, `responseMode=LLM` e `llmProvider=OPENAI`;
5. pergunta de humano retorna `handoffRequired=true`;
6. trace da conversa retorna eventos de recuperação e resposta.

## O Que Não Deve Ir Para O Frontend

- `OPENAI_API_KEY`;
- tokens de gateway;
- senha de banco;
- edição de `.env`;
- lógica de prompt;
- cálculo de embeddings;
- acesso direto ao PostgreSQL;
- integração direta com Meta/WhatsApp sem gateway próprio.

## Próximo Projeto Sugerido

Nome sugerido:

```text
opiagile-rag-demo-web
```

Stack sugerida:

- Vite + Vue ou React;
- CSS próprio leve;
- proxy local para API;
- build estático;
- variáveis `VITE_*`;
- sem autenticação na primeira versão local;
- autenticação simples/token apenas quando virar demo pública.
