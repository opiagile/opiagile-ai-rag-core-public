# Opiagile AI RAG Core

Core de RAG da Opiagile para ingestão de documentos, embeddings, recuperação com fontes, geração conversacional com LLM opcional, memória, handoff e observabilidade.

Este repositório é o backend/API. Interfaces visuais e canais externos devem evoluir em repositórios separados.

## Posicionamento

`opiagile-ai-rag-core` é um motor reutilizável de RAG para produtos de atendimento, suporte e busca assistida sobre documentos.

Ele não é mais o projeto de interface visual. A antiga Demo UI foi removida deste core e documentada para servir de base para um novo frontend separado:

- [Handoff para frontend separado](docs/frontend-handoff.md)

Gateways de canal também devem ficar fora deste core. O primeiro gateway planejado é:

```text
opiagile-whatsapp-ai-gateway
```

## Estado Atual

- Backend Java 25 LTS com Spring Boot.
- PostgreSQL com pgvector.
- Upload de documentos `.txt`.
- Chunking e persistência de chunks.
- Embeddings OpenAI opcionais.
- Recuperação pgvector quando chunks possuem vetores.
- Fallback textual local quando não há embeddings/chave.
- Geração de resposta por LLM opcional com OpenAI.
- Modo local `DEMO` sem chave externa.
- Respostas com fontes.
- Memória conversacional.
- Triagem de intenção.
- Lead e handoff humano.
- Trace por conversa.
- Isolamento por tenant/workspace.
- API keys tenant-aware com escopos por cliente/gateway.
- Ferramentas controladas por workspace, começando por SQL somente leitura.
- Deploy Oracle Cloud Always Free via Docker Compose.

Frase técnica correta:

> Core RAG com fontes, fallback textual local, pgvector com embeddings reais quando configurado e modo local sem chave externa.

## Repositório

Nome lógico do projeto:

```text
opiagile-ai-rag-core
```

URL do repositório:

```text
https://github.com/opiagile/opiagile-ai-rag-core
```

## Stack

| Camada | Tecnologia |
| --- | --- |
| API | Java 25 LTS, Spring Boot 4.1.x, Spring AI 2.0.x, Maven |
| RAG | Recuperação textual local, embeddings OpenAI opcionais, pgvector |
| Banco | PostgreSQL + pgvector |
| Migrations | Flyway |
| LLM | OpenAI opcional, fallback local DEMO |
| Deploy | Docker Compose, Caddy, Oracle Cloud Always Free |
| Automação | Exemplos n8n preservados como referência |

## Estrutura

```text
.
├── backend/                 # API core RAG em Spring Boot
├── deploy/oracle-free-tier/ # Deploy Docker Compose para VPS Oracle ARM64
├── docs/                    # Arquitetura, contratos, avaliação e handoffs
├── samples/                 # Bases de conhecimento de exemplo
├── scripts/                 # Diagnóstico, E2E e validações
├── workflows/n8n/           # Exemplos de automação
├── docker-compose.yml       # Infra local
├── .env.example             # Template seguro
├── AGENTS.md
├── ARCHITECTURE.md
├── ROADMAP.md
└── ISSUES.md
```

## Execução Local

```bash
cp .env.example .env
docker compose --env-file .env up -d postgres
cd backend
./mvnw spring-boot:run
```

Valide:

```bash
python3 - <<'PY'
import urllib.request
for url in ("http://localhost:8080/actuator/health", "http://localhost:8080/api/version"):
    with urllib.request.urlopen(url, timeout=20) as r:
        print(url, r.status, r.read().decode())
PY
```

## Testes

```bash
cd backend
./mvnw test
```

Validação E2E local:

```bash
API_BASE_URL=http://localhost:8080 scripts/e2e-demo.sh
```

## Endpoints Principais

```text
GET  /actuator/health
GET  /api/version
GET  /developers
GET  /developers/console
GET  /developers/api-console/index.html
GET  /developers/swagger-ui/index.html
GET  /v3/api-docs/rag-core
GET  /api/workspaces
POST /api/documents/upload
GET  /api/documents
GET  /api/documents/{id}
GET  /api/documents/{id}/chunks
POST /api/chat
GET  /api/conversations/{id}/messages
GET  /api/conversations/{id}/summary
GET  /api/handoffs
GET  /api/handoffs/{id}
PATCH /api/handoffs/{id}/status
GET  /api/observability/conversations/{id}/trace
GET  /api/providers/status
POST /api/site-chat
GET  /api/tools
POST /api/tools/{slug}/sql/query
```

Endpoints legados de WhatsApp ainda existem como referência técnica para futura extração, mas não são o foco evolutivo deste core.

## Portal De Desenvolvedores

O core expõe uma experiência interativa para devs e empresas validarem o contrato da API:

```text
GET /developers
GET /developers/console
GET /v3/api-docs/rag-core
```

`/developers` é um portal visual com posicionamento, exemplos e instruções de autenticação. `/developers/console` abre o console Opiagile com navegação lateral, seleção de endpoint, preenchimento de parâmetros e execução interativa usando `X-OPIAGILE-API-KEY`.

O portal também possui um formulário de solicitação de API key sandbox. O formulário registra a solicitação no banco em `developer_access_requests`; a liberação da chave é feita por endpoint administrativo protegido, com controle de tenant, workspace, escopos e limite de uso. A chave completa é retornada apenas uma vez ao painel administrativo e também pode ser entregue ao lead por um link de uso único enviado por email. O banco armazena somente prefixo, hash da API key e chave criptografada temporária para entrega segura.

Para leads externos, o fluxo recomendado é criar um sandbox temporário pelo painel interno. Nesse modo, o core cria tenant/workspace com o nome do cliente e expiração de 24h, 48h ou 7 dias. Ao expirar, o tenant/workspace e os dados enviados para teste são excluídos do sandbox. A solicitação do lead permanece registrada para contato, auditoria operacional e continuidade comercial, em linha com LGPD.

Opcionalmente, o core envia emails transacionais pelo remetente visível `contato@opiagile.com`:

- confirmação para o lead após a solicitação;
- notificação interna para `contato@opiagile.com`;
- link de uso único para o lead acessar a API key quando o sandbox for aprovado;
- cópia operacional para `contato@opiagile.com` após a aprovação.

O formulário sempre salva primeiro no banco e responde sem depender do SMTP; um scheduler assíncrono tenta enviar depois. Quando o envio passa, a solicitação é marcada como enviada. Quando falha, ela permanece pendente para nova tentativa.

Para ativar, configure SMTP fora do Git:

```text
DEVELOPER_ACCESS_EMAIL_ENABLED=true
DEVELOPER_ACCESS_EMAIL_TO=contato@opiagile.com
DEVELOPER_ACCESS_EMAIL_FROM=contato@opiagile.com
DEVELOPER_ACCESS_EMAIL_PUBLIC_BASE_URL=https://opiagile.com
DEVELOPER_ACCESS_KEY_DELIVERY_EXPIRES_IN_HOURS=24
SPRING_MAIL_HOST=smtp.office365.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=
SPRING_MAIL_PASSWORD=
SPRING_MAIL_SMTP_AUTH=true
SPRING_MAIL_SMTP_STARTTLS_ENABLE=true
SPRING_MAIL_HEALTH_ENABLED=false
DEVELOPER_ACCESS_EMAIL_SCHEDULER_ENABLED=true
DEVELOPER_ACCESS_EMAIL_SCHEDULER_FIXED_DELAY_MS=60000
DEVELOPER_ACCESS_EMAIL_SCHEDULER_BATCH_SIZE=5
DEVELOPER_ACCESS_EMAIL_SCHEDULER_MAX_ATTEMPTS=10
DEVELOPER_ACCESS_EMAIL_SCHEDULER_RETRY_DELAY_MINUTES=15
DEVELOPER_ACCESS_SANDBOX_CLEANUP_ENABLED=true
DEVELOPER_ACCESS_SANDBOX_CLEANUP_FIXED_DELAY_MS=300000
DEVELOPER_ACCESS_SANDBOX_CLEANUP_BATCH_SIZE=25
```

Se SMTP não estiver configurado ou falhar, o formulário continua funcionando e a solicitação fica salva no banco com `notification_email_sent=false`.

Para Microsoft 365, o usuário SMTP precisa ter permissão para enviar como `contato@opiagile.com`. Se `contato@opiagile.com` for alias ou caixa compartilhada, configure `Send as`/permissão equivalente antes de usar esse endereço como `DEVELOPER_ACCESS_EMAIL_FROM`.

O Swagger UI clássico continua disponível como fallback técnico em:

```text
GET /developers/swagger-ui/index.html
```

Em produção, gere chaves específicas por aplicação, cliente ou workspace. Nunca coloque uma API key real em código frontend, print, issue ou documentação pública.

Flags disponíveis:

```text
OPENAPI_ENABLED=true
OPENAPI_UI_ENABLED=true
```

Para ambientes internos que não devem publicar documentação interativa, defina uma ou ambas como `false`.

## Chat Da Landing Opiagile

O endpoint `POST /api/site-chat` atende o webchat institucional da Opiagile. Ele é um contrato server-to-server para a landing, não deve ser chamado diretamente pelo navegador e exige a chave `X-OPIAGILE-API-KEY`.

Configuração esperada:

```text
SITE_CHAT_ENABLED=true
SITE_CHAT_API_KEY=
SITE_CHAT_TENANT=opiagile
SITE_CHAT_WORKSPACE=opiagile-rag
SITE_CHAT_RATE_LIMIT_PER_MINUTE=20
```

O workspace `opiagile/opiagile-rag` é criado por migration com uma base inicial sobre serviços, integrações, demo RAG, segurança/LGPD e implantação. A landing deve guardar a chave apenas no backend/server route e encaminhar perguntas para este endpoint.

O corpo aceita `responseLanguage` para resposta multilíngue:

```json
{
  "message": "Can I use this with WhatsApp?",
  "visitorId": "lead-123",
  "responseLanguage": "ENGLISH"
}
```

Idiomas aceitos: `PORTUGUESE`, `ENGLISH` e `SPANISH`. Para perguntas em inglês ou espanhol, o core expande termos de negócio para português antes da recuperação textual e também mantém chunks seed traduzidos no workspace da Opiagile.

## Tenant E Workspace

O RAG isola documentos, conversas e retrieval por tenant/workspace. Use headers nas chamadas de upload, listagem e chat:

```text
X-Tenant-Id: demo
X-Workspace-Id: clinica-demo
```

Se os headers não forem enviados, o padrão é:

```text
TENANT_DEFAULT_TENANT=demo
TENANT_DEFAULT_WORKSPACE=clinica-demo
```

Workspaces demo criados por migration:

| Tenant | Workspace | Uso |
| --- | --- | --- |
| `demo` | `clinica-demo` | Clínica ou consultório |
| `demo` | `atendimento-demo` | Atendimento geral, suporte e handoff |
| `demo` | `locacao-demo` | Locação imobiliária |

Liste os workspaces disponíveis:

```bash
curl http://localhost:8080/api/workspaces
```

## Autorização Por API Key

A partir da v0.10, gateways e aplicações externas podem usar API keys vinculadas a um tenant/workspace. Esse é o caminho recomendado para ambientes expostos, porque o core deixa de depender de headers livres enviados pelo cliente.

Configuração:

```text
API_SECURITY_ENABLED=true
API_SECURITY_REQUIRE_API_KEY=false
API_SECURITY_DEFAULT_RATE_LIMIT_PER_MINUTE=60
```

Para compatibilidade local, `API_SECURITY_REQUIRE_API_KEY=false` mantém o comportamento legado com `X-Tenant-Id` e `X-Workspace-Id`. Em ambiente publicado ou multiempresa, use `API_SECURITY_REQUIRE_API_KEY=true` e cadastre clients na tabela `api_clients`.

Gere uma chave e o SQL de cadastro:

```bash
scripts/generate-api-client-key.sh demo clinica-demo "Gateway Demo" "chat:write,documents:read,documents:upload,conversations:read,observability:read,providers:read,workspaces:read" 60
```

O script imprime a API key uma única vez e o SQL com o hash SHA-256. A chave real não deve ser commitada nem armazenada em texto puro.

Use a chave nas chamadas:

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -H "X-OPIAGILE-API-KEY: $OPIAGILE_CORE_API_KEY" \
  -d '{
    "message": "Vocês atendem aos sábados?",
    "channel": "WEB",
    "contactId": "lead-demo"
  }'
```

Escopos disponíveis:

| Escopo | Permite |
| --- | --- |
| `chat:write` | Enviar mensagens para `POST /api/chat` |
| `documents:read` | Listar documentos, detalhes e chunks |
| `documents:upload` | Enviar documentos TXT |
| `conversations:read` | Ler mensagens e resumo de conversas |
| `observability:read` | Consultar trace de conversa |
| `handoffs:read` | Listar handoffs |
| `handoffs:write` | Atualizar status de handoff |
| `tools:read` | Listar ferramentas do workspace |
| `tools:execute` | Executar ferramenta controlada, como SQL read-only |
| `providers:read` | Consultar status seguro de provedores IA e fallback |
| `workspaces:read` | Listar workspaces permitidos |

Quando uma API key válida é usada, o tenant/workspace vêm da própria credencial e os headers `X-Tenant-Id` e `X-Workspace-Id` são ignorados. Isso evita que um gateway ou usuário altere o escopo de dados livremente.

### Auditoria De API Clients

A v0.11 registra uso operacional das API keys em `api_client_usage_logs`, incluindo client, tenant/workspace, endpoint, escopo, status HTTP, bloqueio, IP, user agent e latência. A chave real e payloads não são registrados.

Consulte o relatório de uso com token administrativo:

```bash
curl http://localhost:8080/api/admin/api-clients/usage \
  -H "X-Demo-Admin-Token: $DEMO_ADMIN_TOKEN"
```

O endpoint retorna resumo por client e eventos recentes. Use `?limit=100` para ajustar a quantidade, limitada a 200.

## Ferramentas Controladas

O core suporta o cadastro de ferramentas por tenant/workspace para expor capacidades de negócio ao assistente sem misturar integrações de canal no backend principal.

A primeira ferramenta implementada é `SQL_READ_ONLY`, voltada para consultas controladas em modo somente leitura. Ela usa:

- allowlist de tabelas por ferramenta;
- apenas consultas `SELECT` simples;
- bloqueio de comandos de escrita, comentários e múltiplos comandos;
- limite de linhas por ferramenta;
- log de execução sem registrar segredos;
- proteção por token administrativo por padrão.

Liste as ferramentas do workspace:

```bash
curl http://localhost:8080/api/tools \
  -H "X-Tenant-Id: opiagile" \
  -H "X-Workspace-Id: opiagile-rag"
```

Execute uma consulta read-only autorizada:

```bash
curl -X POST http://localhost:8080/api/tools/base-conhecimento-readonly/sql/query \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: opiagile" \
  -H "X-Workspace-Id: opiagile-rag" \
  -H "X-Demo-Admin-Token: $DEMO_ADMIN_TOKEN" \
  -d '{
    "sql": "select filename, status, created_at from documents order by created_at desc",
    "maxRows": 10
  }'
```

Essa base segue a mesma ideia de skills empresariais: cada workspace pode declarar quais capacidades existem, quais dados podem ser consultados e quais limites de segurança devem ser respeitados. Gateways como Slack, Teams, WhatsApp ou webchat podem consumir esse contrato sem receber credenciais diretas.

O fluxo de chat também consegue usar resultados dessas ferramentas em casos controlados. Nesta versão, o planner interno executa consultas pré-aprovadas sobre a base de conhecimento do workspace quando a pergunta pede informações como:

- quantidade de documentos indexados;
- lista de arquivos/documentos disponíveis;
- quantidade de trechos por documento;
- consultas recentes registradas em `retrieval_logs`.

Esses resultados entram no contexto da resposta do assistente. O usuário final não envia SQL pelo chat, e a LLM não tem permissão para executar consultas livres.

Quando `TOOLS_PLANNER_LLM_ENABLED=true` e `OPENAI_API_KEY` estiver configurada, o core pode usar a LLM como classificador de intenção de ferramenta. Mesmo nesse modo, a LLM só escolhe uma ação permitida (`DOCUMENT_COUNT`, `DOCUMENT_LIST`, `CHUNK_STATS`, `RECENT_RETRIEVALS` ou `NONE`). O SQL continua sendo montado pelo backend e validado pelo guard read-only.

## Upload De Documento

Por segurança, a demo limita uploads antes de indexar o conteúdo. Os limites padrão são:

```text
DOCUMENT_UPLOAD_MAX_BYTES=262144
DOCUMENT_UPLOAD_MAX_CHARS=200000
DOCUMENT_UPLOAD_MAX_CHUNKS=300
DOCUMENT_UPLOAD_MAX_DOCUMENTS=50
SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=256KB
SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=300KB
```

Também são aplicados UTF-8 obrigatório, sanitização de nome de arquivo e rejeição de arquivos que gerem chunks demais.

## Proteção De Demo

O core pode rodar localmente sem autenticação. Para exposição externa de demonstração, configure token e limites no `.env`:

```text
DEMO_ACCESS_TOKEN=
DEMO_ADMIN_TOKEN=
DEMO_RATE_LIMIT_ENABLED=true
DEMO_CHAT_RATE_LIMIT_PER_MINUTE=30
DEMO_UPLOAD_RATE_LIMIT_PER_MINUTE=5
```

Quando `DEMO_ACCESS_TOKEN` estiver preenchido, `POST /api/chat` e `POST /api/documents/upload` exigem `X-Demo-Token` ou `Authorization: Bearer`.

Quando `DEMO_ADMIN_TOKEN` estiver preenchido, o reset protegido fica disponível:

```bash
curl -X POST http://localhost:8080/api/admin/demo/reset \
  -H "X-Demo-Admin-Token: TOKEN_ADMIN_LOCAL"
```

Esse endpoint remove dados de demonstração, conversas, mensagens, handoffs, logs, eventos WhatsApp e documentos. Nunca use token real em commits, prints ou documentação pública.

```bash
python3 - <<'PY'
import pathlib
import urllib.request
import uuid

boundary = "----opiagile" + uuid.uuid4().hex
path = pathlib.Path("samples/clinica/faq.txt")
body = (
    f"--{boundary}\r\n"
    f'Content-Disposition: form-data; name="file"; filename="{path.name}"\r\n'
    "Content-Type: text/plain\r\n\r\n"
).encode() + path.read_bytes() + f"\r\n--{boundary}--\r\n".encode()
request = urllib.request.Request(
    "http://localhost:8080/api/documents/upload",
    data=body,
    headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
    method="POST",
)
print(urllib.request.urlopen(request).read().decode())
PY
```

## Chat RAG

```bash
python3 - <<'PY'
import json
import urllib.request

payload = json.dumps({
    "message": "Vocês atendem aos sábados?",
    "channel": "WEB",
    "contactId": "teste-local",
    "responseLanguage": "PORTUGUESE"
}).encode()

request = urllib.request.Request(
    "http://localhost:8080/api/chat",
    data=payload,
    headers={"Content-Type": "application/json"},
    method="POST",
)
print(urllib.request.urlopen(request).read().decode())
PY
```

## Modo LLM E Embeddings

Sem chave externa:

```text
LLM_PROVIDER=DEMO
CHAT_RESPONSE_MODE=DEMO
OPENAI_EMBEDDINGS_ENABLED=false
TOOLS_PLANNER_LLM_ENABLED=true
```

Com OpenAI:

```text
LLM_PROVIDER=OPENAI
CHAT_RESPONSE_MODE=LLM
OPENAI_API_KEY=
OPENAI_CHAT_MODEL=gpt-5-mini
OPENAI_EMBEDDINGS_ENABLED=true
OPENAI_EMBEDDINGS_PROVIDER=manual
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
OPENAI_EMBEDDING_DIMENSIONS=1536
TOOLS_PLANNER_LLM_ENABLED=true
```

Provider de embeddings:

- `OPENAI_EMBEDDINGS_PROVIDER=manual`: provider atual com `RestClient` próprio.
- `OPENAI_EMBEDDINGS_PROVIDER=spring-ai`: POC v0.14 usando `EmbeddingModel` do Spring AI. Para ativar, configure também `SPRING_AI_MODEL_EMBEDDING=openai`.

Nunca commite `.env` ou chaves reais.

## Resiliência De Provedores

A v0.13 adiciona um endpoint seguro para verificar o estado operacional de LLM, embeddings, recuperação e planner sem expor segredos:

```bash
curl http://localhost:8080/api/providers/status
```

O retorno informa provider ativo, modo solicitado, presença booleana de chave, fallback configurado e avisos operacionais. O endpoint não chama provedores externos e não imprime `OPENAI_API_KEY`.

## Deploy Oracle

Guia completo:

- [Deploy Oracle Cloud Always Free](docs/oracle-free-tier-deploy.md)

O deploy atual usa Docker Compose, Caddy e PostgreSQL/pgvector. A pipeline de `develop` valida backend, Compose e scanner de segredos antes de atualizar a VPS.

Política operacional:

- `main`: linha estável, sem deploy automático.
- `develop`: ambiente Oracle/demo, com deploy automático.
- [Política de branches e deploy](docs/branching-deploy-policy.md)
- [Painel interno de administração](docs/internal-admin-panel.md)

## Frontend Separado

A interface visual foi removida deste repositório. Para criar o novo front, use:

- [Handoff para frontend separado](docs/frontend-handoff.md)

Nome sugerido para o novo projeto:

```text
opiagile-rag-demo-web
```

## Gateway Separado

Integrações com WhatsApp, Teams, Slack, CRM ou outros canais devem consumir este core por HTTP.

Documento de contrato:

- [Contrato para gateways](docs/gateway-contract.md)

Nome sugerido para o primeiro gateway:

```text
opiagile-whatsapp-ai-gateway
```

## Documentação Principal

- [Arquitetura executiva](ARCHITECTURE.md)
- [Arquitetura técnica](docs/architecture.md)
- [Exemplos de API](docs/api-examples.md)
- [Estratégia de avaliação RAG](docs/rag-evaluation-strategy.md)
- [Handoff para frontend separado](docs/frontend-handoff.md)
- [Contrato para gateways](docs/gateway-contract.md)
- [Deploy Oracle](docs/oracle-free-tier-deploy.md)
- [Painel interno de administração](docs/internal-admin-panel.md)
- [Relatório v0.6 RAG core pgvector](docs/v0.6-rag-core-pgvector-report.md)
- [Relatório v0.7 core sem frontend](docs/v0.7-core-sem-frontend-report.md)
- [Relatório v0.13 resiliência de provedores](docs/v0.13-provider-resilience-report.md)
- [Relatório v0.12 portal de desenvolvedores](docs/v0.12-developer-portal-openapi-report.md)
- [Relatório v0.15.1 fluxo de branches e deploy](docs/v0.15.1-branch-deploy-oracle-report.md)
- [Relatório v0.14 code review Spring AI](docs/v0.14-spring-ai-code-review.md)
- [Relatório v0.14 POC Spring AI](docs/v0.14-spring-ai-poc-report.md)
- [Relatório v0.15 Java 25 LTS](docs/v0.15-java25-lts-report.md)
- [Relatório v0.16 API key sandbox admin](docs/v0.16-api-key-sandbox-admin-report.md)
- [Relatório v0.17 email de entrega de API key sandbox](docs/v0.17-email-entrega-api-key-report.md)
- [Relatório v0.16.1 painel interno de administração](docs/v0.16.1-internal-admin-panel-report.md)

## Licença

MIT. Veja [LICENSE](LICENSE).
