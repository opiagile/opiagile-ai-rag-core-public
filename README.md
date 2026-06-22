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

- Backend Java 21 com Spring Boot.
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
| API | Java 21, Spring Boot 3.5.x, Maven |
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
```

Endpoints legados de WhatsApp ainda existem como referência técnica para futura extração, mas não são o foco evolutivo deste core.

## Upload De Documento

Por segurança, a demo limita uploads antes de indexar o conteúdo. Os limites padrão são:

```text
DOCUMENT_UPLOAD_MAX_BYTES=262144
DOCUMENT_UPLOAD_MAX_CHARS=200000
DOCUMENT_UPLOAD_MAX_CHUNKS=300
SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=256KB
SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=300KB
```

Também são aplicados UTF-8 obrigatório, sanitização de nome de arquivo e rejeição de arquivos que gerem chunks demais.

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
    "contactId": "teste-local"
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
```

Com OpenAI:

```text
LLM_PROVIDER=OPENAI
CHAT_RESPONSE_MODE=LLM
OPENAI_API_KEY=
OPENAI_CHAT_MODEL=gpt-5-mini
OPENAI_EMBEDDINGS_ENABLED=true
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
OPENAI_EMBEDDING_DIMENSIONS=1536
```

Nunca commite `.env` ou chaves reais.

## Deploy Oracle

Guia completo:

- [Deploy Oracle Cloud Always Free](docs/oracle-free-tier-deploy.md)

O deploy atual usa Docker Compose, Caddy e PostgreSQL/pgvector. A pipeline de `develop` valida backend, Compose e scanner de segredos antes de atualizar a VPS.

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
- [Relatório v0.6 RAG core pgvector](docs/v0.6-rag-core-pgvector-report.md)
- [Relatório v0.7 core sem frontend](docs/v0.7-core-sem-frontend-report.md)

## Licença

MIT. Veja [LICENSE](LICENSE).
