# Lista De Tarefas

## Lembrete Visível — Email De Solicitações Developer

- [ ] Retomar envio automático de email das solicitações de API key sandbox.

Contexto: o formulário `/developers` já salva as solicitações no banco e a fila assíncrona já existe. O envio ficou temporariamente desabilitado na VPS porque o Microsoft 365 recusou autenticação SMTP com `MailAuthenticationException`.

Próximas opções:

- habilitar SMTP AUTH/app password na conta correta do Microsoft 365; ou
- trocar o envio para Microsoft Graph API com credenciais seguras; ou
- usar provedor transacional dedicado para emails operacionais.

Critério para concluir: `DEVELOPER_ACCESS_EMAIL_ENABLED=true`, envio validado sem bloquear o lead, `notification_email_sent=true` após sucesso e retries preservados em caso de falha.

## Lembrete Visível — v0.14 Spring AI

- [x] Registrar code review v0.14 de Spring AI.
- [x] Implementar POC inicial `SpringAiEmbeddingProvider`.
- [ ] Avaliar `SpringAiChatModelProvider`.
- [ ] Comparar provider manual versus Spring AI com dataset RAG.
- [ ] Decidir sobre `PgVectorStore`, advisors e tool calling.

Contexto: o relatório [`docs/v0.14-spring-ai-code-review.md`](docs/v0.14-spring-ai-code-review.md) confirma que o core usa bem Spring Boot, Spring Web, Validation, JDBC, Flyway, Actuator, Mail, Scheduler e springdoc, mas ainda não usa Spring AI diretamente para chat, embeddings, vector store, advisors ou tool calling.

Próxima decisão técnica: antes de adicionar novas capacidades de IA, avaliar se `OpenAiChatModelProvider`, `OpenAiEmbeddingProvider`, prompt/advisors, pgvector e tool calling devem migrar parcialmente para Spring AI ou permanecer manuais por necessidade de controle.

Critério para concluir totalmente a trilha Spring AI: prova de conceito de chat validada, decisão registrada, testes passando e contrato público preservado.

## Fundação

- [x] Criar arquivos de fundação do repositório.
- [x] Adicionar scripts com atenção a ARM64.
- [x] Adicionar bases de conhecimento de exemplo.
- [x] Adicionar Docker Compose e `.env.example`.
- [x] Escolher licença MIT do repositório.

## API

- [x] Criar API Java 21 com Spring Boot.
- [x] Adicionar Actuator e `/api/version`.
- [x] Adicionar Maven wrapper e testes.
- [x] Configurar Flyway e conexão PostgreSQL.

## Banco E RAG

- [x] Criar migration da extensão pgvector.
- [x] Criar schema inicial.
- [x] Implementar upload de TXT.
- [x] Implementar chunking de documentos.
- [x] Adicionar abstração de provedor de embeddings.
- [x] Implementar recuperação textual/local demonstrável.
- [x] Retornar fontes e registrar logs de recuperação.
- [x] Validar geração conversacional com LLM real quando OpenAI está configurada.
- [x] Implementar embeddings reais e persistência em `document_chunks.embedding`.
- [x] Implementar recuperação vetorial pgvector com fallback textual local.
- [ ] Automatizar avaliação RAG com dataset de perguntas, respostas e fontes esperadas.

## Automação De Atendimento

- [x] Persistir conversas e mensagens.
- [x] Implementar classificador de intenção.
- [x] Implementar extração de lead.
- [x] Implementar regras de handoff.
- [x] Adicionar endpoints de handoff.
- [x] Documentar contrato para frontend separado após remoção da Demo UI do core.

## Canais E Fluxos

- [x] Adicionar webhook WhatsApp simulado.
- [x] Adicionar abstrações de provedor.
- [x] Implementar piloto controlado WhatsApp Cloud API com dry-run, assinatura, allowlist e rate limit.
- [x] Criar checklist e script seguro para primeiro teste real WhatsApp.
- [x] Definir que canais devem evoluir em gateways separados, começando por `opiagile-whatsapp-ai-gateway`.
- [x] Adicionar exemplos JSON de fluxos n8n.
- [x] Documentar payloads de webhook.
- [ ] Validar primeiro recebimento real WhatsApp com número autorizado em dry-run.
- [ ] Validar primeiro envio real WhatsApp somente para número do responsável.

## Observabilidade

- [x] Registrar resultados de recuperação.
- [x] Adicionar endpoint de rastro de conversa.
- [x] Registrar metadados de resposta DEMO/LLM.
- [x] Preparar integração opcional com Langfuse.
- [x] Documentar estratégia de avaliação RAG.

## Portfólio

- [x] Adicionar documentação detalhada de casos de uso.
- [x] Adicionar coleção de exemplos de API.
- [x] Polir README com marcadores provisórios de screenshots/GIFs.
- [x] Remover interface gráfica do core.
- [x] Criar `docs/frontend-handoff.md` para orientar novo projeto front.
- [x] Adicionar relatório final de validação.

## Próxima Fase Recomendada

- [x] v0.13 — Resiliência de provedores IA.
- [x] v0.14 — Code review e POC inicial com Spring AI para embeddings.
- [ ] v0.14.x — Avaliação de chat, advisors, PgVectorStore e tool calling com Spring AI.
- [ ] Avaliação RAG automatizada com dataset de perguntas, respostas e fontes esperadas.

Descrição: criar um conjunto versionado de perguntas por nicho, respostas esperadas, fontes esperadas e pontuação automática/semi-automática para medir qualidade de recuperação textual, recuperação pgvector e resposta final com LLM.

O teste real WhatsApp fica fora da trilha principal deste core e deve continuar em aplicação gateway separada.

## Pendências Técnicas E Comerciais

- [ ] Primeiro envio real WhatsApp com tester autorizado em gateway separado.
- [ ] Avaliação RAG automatizada.
- [ ] Demo pública com HTTPS.
- [ ] Autenticação ou limites de uso para demo pública.
- [ ] Criar frontend separado a partir de `docs/frontend-handoff.md`.
- [ ] Ambiente demo resetável para potenciais clientes.
- [ ] Revisão de segurança/LGPD antes de qualquer uso produtivo.
