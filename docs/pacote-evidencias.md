# Pacote De Evidências Do Projeto

Este documento organiza quais materiais podem ser enviados para comprovar o estado atual do `opiagile-ai-rag-core`.

## Link Principal

Repositório atual:

```text
https://github.com/opiagile/opiagile-ai-rag-core-public
```

Nome lógico do projeto:

```text
opiagile-ai-rag-core
```

## Resumo Executivo

O projeto é um core RAG da Opiagile para ingestão de documentos, embeddings opcionais, recuperação pgvector, fallback textual local, respostas com fontes, geração conversacional opcional com LLM, memória, triagem, lead, handoff e observabilidade.

Ele roda localmente sem chaves externas em modo `DEMO`. Com OpenAI configurada, gera embeddings reais, recupera por pgvector quando os chunks possuem vetores e responde com LLM usando histórico, intenção, lead, handoff e fontes recuperadas.

Frase técnica correta:

> Core RAG com fontes, fallback textual local, pgvector com embeddings reais quando configurado e modo local sem chave externa.

## Estado Atual Em Uma Frase

Core público da Opiagile para RAG com fontes, LLM real validada, embeddings/pgvector opcionais, memória conversacional, triagem, handoff, observabilidade e contrato para frontend/gateways separados.

## Documentos Recomendados Para Enviar

### 1. README Principal

Arquivo: [`README.md`](../README.md)

Use quando quiser apresentar o projeto de forma geral.

Comprova:

- escopo de core API;
- stack técnica;
- endpoints implementados;
- modo local sem chave externa;
- LLM opcional;
- embeddings e pgvector quando configurados;
- links para documentação complementar.

Prioridade: alta.

### 2. Arquitetura Técnica

Arquivos:

- [`ARCHITECTURE.md`](../ARCHITECTURE.md)
- [`docs/architecture.md`](architecture.md)

Comprova:

- desenho do fluxo RAG;
- separação entre core, frontend e gateways;
- decisões de fallback textual, pgvector e LLM opcional;
- limites do que é produção, demo e portfólio.

Prioridade: alta.

### 3. Relatório RAG Core pgvector

Arquivo: [`docs/v0.6-rag-core-pgvector-report.md`](v0.6-rag-core-pgvector-report.md)

Comprova:

- embeddings reais opcionais;
- persistência em `document_chunks.embedding`;
- recuperação pgvector quando configurada;
- fallback textual local;
- validação técnica da v0.6.

Prioridade: alta para conversas sobre RAG.

### 4. Relatório LLM Real

Arquivo: [`docs/v0.4.1-llm-real-validation-report.md`](v0.4.1-llm-real-validation-report.md)

Comprova:

- execução com `responseMode=LLM`;
- provider OpenAI;
- modelo testado;
- respostas com fontes;
- avaliação manual de fluidez;
- higiene de segredos.

Prioridade: alta para conversas sobre IA generativa.

### 5. Handoff Para Frontend Separado

Arquivo: [`docs/frontend-handoff.md`](frontend-handoff.md)

Use quando quiser passar a construção da interface para outro agente ou outro repositório.

Comprova:

- endpoints consumidos;
- payloads de request/response;
- estados de UI esperados;
- regras de UX;
- validação contra API local/VPS;
- limites de segurança do frontend.

Prioridade: alta para iniciar `opiagile-rag-demo-web`.

### 6. Contrato Para Gateways

Arquivo: [`docs/gateway-contract.md`](gateway-contract.md)

Use quando quiser integrar WhatsApp, Teams, Slack, CRM ou webchat.

Comprova:

- contrato HTTP esperado para canais externos;
- campos mínimos de request/response;
- separação entre core RAG e gateway de canal.

Prioridade: alta para iniciar `opiagile-whatsapp-ai-gateway`.

### 7. Deploy Oracle

Arquivo: [`docs/oracle-free-tier-deploy.md`](oracle-free-tier-deploy.md)

Comprova:

- deploy em VPS Oracle ARM64;
- Docker Compose;
- PostgreSQL/pgvector local;
- Caddy;
- backup;
- pipeline `develop`.

Prioridade: alta para demonstrar execução fora da máquina local.

### 8. Exemplos De API

Arquivo: [`docs/api-examples.md`](api-examples.md)

Comprova:

- upload de documento;
- chat RAG;
- handoff;
- trace;
- status de endpoints.

Prioridade: média/alta para desenvolvedores.

## O Que Já Pode Ser Dito

- O projeto é um core RAG funcional.
- A camada LLM foi validada com OpenAI.
- Embeddings reais e recuperação pgvector funcionam quando configurados.
- O modo local sem chave continua funcionando.
- O frontend foi removido do core e documentado para virar projeto separado.
- Canais externos devem ser implementados como gateways separados.

## O Que Não Deve Ser Dito

- Não dizer que é produção.
- Não dizer que há demo pública aberta.
- Não dizer que o frontend está neste repositório.
- Não vender WhatsApp/Teams como parte principal deste core.
- Não dizer que dados reais podem ser enviados sem revisão de segurança/LGPD.

## Próximos Passos Recomendados

1. Criar `opiagile-rag-demo-web` a partir de `docs/frontend-handoff.md`.
2. Criar `opiagile-whatsapp-ai-gateway` a partir de `docs/gateway-contract.md`.
3. Automatizar avaliação RAG com dataset de perguntas, respostas e fontes esperadas.
4. Adicionar autenticação/rate limit antes de qualquer demo pública.
5. Preparar domínio e HTTPS para uso controlado.
