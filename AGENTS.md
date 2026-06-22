# AGENTS.md

Instruções permanentes para agentes que trabalharem no repositório `opiagile-ai-rag-core`.

## Missão

Construir um core público de RAG da Opiagile para ingestão de documentos, recuperação com fontes, memória conversacional, resposta com LLM opcional, observabilidade, avaliação e contrato HTTP para gateways externos.

Este repositório deve parecer um acelerador real de produto para RAG, não uma demonstração de aplicação genérica.

Integrações de canal, como WhatsApp, Teams, Slack ou CRM, devem evoluir em aplicações próprias. O piloto WhatsApp já implementado fica preservado como referência técnica até futura extração para `opiagile-whatsapp-ai-gateway`.

Interfaces gráficas não devem ser desenvolvidas neste repositório. O estado da antiga Demo UI foi documentado em `docs/frontend-handoff.md` para orientar um projeto frontend separado.

## Idioma

- Toda documentação, descrições de tarefas, mensagens de commit, comentários explicativos e textos voltados ao GitHub devem estar em português.
- Identificadores técnicos podem permanecer no idioma exigido pela ferramenta, por exemplo nomes de variáveis, comandos, endpoints, pacotes e tipos de commit convencionais.
- Evitar textos em inglês em README, docs, scripts e exemplos, salvo quando for nome próprio de tecnologia, API, endpoint, pacote, pasta ou comando.

## Ambiente Alvo

- Linux ARM64/aarch64 em VM Parallels.
- Priorizar imagens Docker e pacotes com suporte a Linux ARM64.
- Não assumir ambiente x86_64.
- API: Java 21, Maven, Spring Boot 3.5.x e Spring AI 1.1.x.
- Banco: PostgreSQL com pgvector.
- Frontend: fora do escopo deste repositório. Use `docs/frontend-handoff.md` para criar projeto separado.

## Comandos De Compilação E Teste

Executar a partir da raiz do repositório, salvo indicação em contrário.

```bash
bash scripts/check-env.sh
bash scripts/bootstrap-local.sh
cp .env.example .env
docker compose --env-file .env config
docker compose --env-file .env up -d
```

Quando a API existir:

```bash
cd backend
./mvnw test
./mvnw spring-boot:run
```

## Convenções De Arquitetura

- Controllers apenas orquestram request/response.
- Services concentram regra de negócio.
- Repositories cuidam da persistência.
- DTOs devem ficar separados das entidades de persistência.
- Lógica de prompt não deve ficar em controllers.
- Criar abstrações para provedores externos:
  - `EmbeddingProvider`
  - `ChatModelProvider`
  - `WhatsAppProvider`
  - `IntentClassifier`
- O modo demonstração deve funcionar sem chaves reais.
- O modo real deve ser ativado por variáveis de ambiente.
- Manter módulos pequenos, legíveis e testáveis.

Pacote base sugerido para a API:

```text
com.opiagile.supportai
```

Domínios sugeridos para a API:

```text
config, common, version, document, rag, chat, conversation, lead, webhook, handoff, observability
```

## Política De Segredos

- Nunca commitar credenciais reais, tokens, chaves privadas, `.env` ou arquivos sensíveis gerados localmente.
- Usar `.env.example` apenas com nomes de variáveis e valores seguros de desenvolvimento.
- Chaves como `OPENAI_API_KEY`, `WHATSAPP_ACCESS_TOKEN`, `WHATSAPP_APP_SECRET`, `WHATSAPP_VERIFY_TOKEN` e chaves Langfuse devem ser opcionais e nunca registradas em logs ou documentação.
- Se uma ação depender de credenciais reais, parar e pedir intervenção humana.

## Compatibilidade ARM64

- Preferir imagens Docker com variantes publicadas para Linux ARM64.
- Evitar ferramentas que distribuam apenas binários x86_64, salvo quando forem opcionais.
- Validar com `uname -m`; no ambiente alvo o retorno esperado é `aarch64`.
- Serviços do Compose não devem fixar `platform`, exceto com justificativa clara.

## Critérios De Qualidade

- O projeto deve rodar localmente em modo demonstração sem serviços pagos.
- README e documentação precisam acompanhar a implementação atual.
- Adicionar testes para serviços críticos.
- Evitar overengineering e dependências desnecessárias.
- Não copiar grandes trechos de projetos terceiros.
- Respeitar licenças das bibliotecas usadas.
- Validar antes de commitar: scripts, configuração Docker Compose e testes/builds dos módulos existentes.

## Padrão De Commits

Usar prefixos convencionais, com descrição em português:

- `chore:` configuração, tooling e manutenção
- `feat:` funcionalidade de produto
- `fix:` correções
- `docs:` documentação
- `test:` testes
- `refactor:` reorganização sem mudança de comportamento

Fazer commits pequenos e validados ao fim de cada fase.

## Validação Por Módulo

- Fundação do repositório: `bash scripts/check-env.sh` e `docker compose --env-file .env config`.
- API: `./mvnw test`, depois verificar `/actuator/health` e `/api/version` com a aplicação rodando.
- Banco: `docker compose --env-file .env up -d postgres`, depois verificar healthcheck e migrations Flyway pela API.
- Ingestão de documentos: enviar exemplos, listar documentos e inspecionar chunks.
- Chat RAG: fazer perguntas sobre os exemplos e verificar respostas com fontes.
- Webhook WhatsApp: enviar payload simulado e verificar conversa, lead, intenção e resposta.
- Frontend: não validar neste repositório; qualquer interface deve estar em projeto separado.


## Sprints De Divulgação

- Sprints de divulgação não devem alterar backend, schema, migrations ou regras de negócio.
- Comunicação deve diferenciar portfólio técnico, demo pública e produção real.
- Evitar promessas exageradas sobre RAG, WhatsApp, produção e deploy.
- Usar linguagem precisa: core RAG com fontes, fallback textual local, pgvector com embeddings reais quando configurado e modo local sem chave externa.

## Pilotos WhatsApp

- O modo `MOCK` deve continuar funcionando sem credenciais.
- `META_CLOUD` deve iniciar em dry-run e envio desabilitado.
- Envio real exige allowlist, consentimento dos testers, HTTPS público, assinatura válida e credenciais fora do Git.
- Nunca exibir tokens, app secret, verify token ou lista completa de telefones na API, UI, logs ou relatórios.
- Novas evoluções de canal devem preferencialmente ir para aplicações gateway separadas, não para este core RAG.

## Diagnóstico WhatsApp

- Use `scripts/check-whatsapp-pilot.sh` para validar configuração local do piloto sem imprimir segredos.
- O script pode carregar `.env` com parser simples, mas nunca deve imprimir tokens, app secret, verify token ou telefones completos.
