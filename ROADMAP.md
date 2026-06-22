# Roteiro

## Fase 0 - Fundação Do Repositório

Objetivo: fazer o repositório parecer um produto de portfólio desde o primeiro commit.

Entregáveis:

- README, roteiro, arquitetura, backlog e roteiro de demonstração.
- `.env.example`, Docker Compose e Makefile.
- Scripts locais com atenção a ARM64.
- Bases de conhecimento de exemplo para clínica, imobiliária e suporte técnico.
- Placeholders para n8n e página.
- `AGENTS.md` com regras permanentes do projeto.

## Fase 1 - API Spring Boot Mínimo

Objetivo: criar uma API Java 21 limpa, compilável e testável.

Entregáveis:

- API Spring Boot 3.5.x com Maven wrapper.
- Endpoint de saúde do Actuator.
- Endpoint `/api/version`.
- `application.yml` baseado em variáveis de ambiente.
- Testes básicos.

## Fase 2 - PostgreSQL + pgvector + Migrations

Objetivo: criar a infraestrutura local de banco vetorial e schema inicial.

Entregáveis:

- PostgreSQL com pgvector via Docker Compose.
- Migration Flyway criando extensão vector e tabelas iniciais.
- Conectividade da API com o banco.

## Fase 3 - Ingestão De Documentos

Status: concluída na primeira versão.

Objetivo: enviar documentos TXT e criar chunks.

Entregáveis:

- Endpoints de upload, listagem e detalhe de documentos.
- Serviço de chunking.
- Abstração de provedor de embeddings.
- Provedor sem ação/demonstração.

## Fase 4 - Chat RAG Com Fontes

Status: concluída na primeira versão e ampliada na v0.6.

Objetivo: responder perguntas usando chunks recuperados dos documentos.

Entregáveis:

- Endpoint `/api/chat`.
- Respostas com fontes.
- Logs de recuperação.
- Fallback demonstração sem API key.
- Embeddings reais opcionais quando configurados.
- Recuperação pgvector com fallback textual local.

## Fase 5 - Memória Conversacional

Status: concluída na primeira versão.

Objetivo: persistir e reutilizar histórico de conversa.

Entregáveis:

- Criação e busca de conversas.
- Persistência de mensagens.
- Contexto com histórico recente.
- Suporte a resumo simples.

## Fase 6 - Triagem De Intenção E Lead

Status: concluída na primeira versão.

Objetivo: sair de “chat com documento” para assistente de atendimento.

Entregáveis:

- Classificador de intenção.
- Implementação local baseada em regras.
- Extração de lead e atualização de status.
- Gatilhos de handoff para intenções críticas.

## Fase 7 - Handoff Humano

Status: concluída na primeira versão.

Objetivo: demonstrar maturidade operacional.

Entregáveis:

- Regras de handoff.
- Geração de resumo para atendimento humano.
- Endpoints de listagem, detalhe e atualização de status.

## Fase 8 - Webhook WhatsApp Simulado

Status: preservada como referência técnica para futura extração.

Objetivo: provar automação por canal externo sem depender de provedor real.

Entregáveis:

- Endpoint de webhook WhatsApp simulado.
- Abstração de provedor.
- Payloads de resposta simulados.
- Mapeamento de conversa por telefone.

Observação: a evolução de canais externos deixa de ser foco deste repositório. O caminho recomendado é extrair WhatsApp para `opiagile-whatsapp-ai-gateway` e manter este projeto como `opiagile-ai-rag-core`.

## Fase 9 - Exemplos De Fluxos n8n

Status: concluída na primeira versão.

Objetivo: adicionar ativos visuais de automação para clientes e freelas.

Entregáveis:

- Fluxo de triagem WhatsApp.
- Fluxo de FAQ com RAG.
- Fluxo de handoff humano.
- Documentação de URLs locais e payloads.

## Fase 10 - Observabilidade E Avaliação

Status: concluída na primeira versão.

Objetivo: mostrar maturidade em LLMOps.

Entregáveis:

- Endpoint de rastro por conversa.
- Detalhes de recuperação e alternativa local.
- Configuração opcional de Langfuse.
- Notas de avaliação de respostas RAG.

## Fase 11 - Casos De Uso Demonstráveis

Status: concluída na primeira versão.

Objetivo: deixar o portfólio concreto.

Entregáveis:

- Samples de clínica, imobiliária e suporte técnico.
- Documentos de caso de uso com perguntas e comportamento esperado.

## Fase 12 - Polimento Da Documentação De Portfólio

Status: concluída na primeira versão.

Objetivo: deixar o repositório pronto para clientes.

Entregáveis:

- README visual.
- Coleção de exemplos de API.
- Roteiro de demonstração.
- Documento de oferta freelancer.

## Fase 13 - Interface Visual Histórica

Status: removida do core após reposicionamento.

Objetivo histórico: criar uma porta de entrada comercial e uma interface local de demonstração.

Entregáveis:

- Antiga landing institucional e Demo UI local foram documentadas antes da remoção.
- O contrato para recriar o frontend separado está em `docs/frontend-handoff.md`.
- Novas interfaces devem viver fora deste repositório.

## Fase 14 - Validação Final

Status: concluída na primeira versão.

Objetivo: provar que o projeto inteiro está apresentável.

Entregáveis:

- Relatório final de validação.
- Comandos confirmados e limitações conhecidas.
- Checklist de ausência de segredos.


## Sprint Divulgação v0.1

Status: concluída nesta revisão de documentação.

Objetivo: alinhar README, roteiros, arquitetura, licença e relatórios para divulgação pública como portfólio técnico.

Entregáveis:

- Licença MIT.
- Documentação sem contradições sobre estado atual.
- URL pública do GitHub consolidada.
- Estado atual validado.
- Estratégia de avaliação RAG.
- Documento de prontidão para demo pública.
- Relatório de prontidão v0.1.

## Sprint Demo UI v0.2

Status: concluída.

Objetivo: criar interface visual simples para cliente não técnico testar upload de documento, pergunta, resposta, fontes, `conversationId`, handoff e trace básico.

Observação: após o reposicionamento para core, a interface foi removida deste repositório e registrada em `docs/frontend-handoff.md`.

## Sprint v0.6 - RAG Core Com Embeddings E pgvector

Status: concluída nesta revisão.

Objetivo: reposicionar o repositório como `opiagile-ai-rag-core`, preservar WhatsApp como referência de gateway e implementar embeddings reais com recuperação pgvector.

## Sprint v0.7 - Core Sem Frontend

Status: concluída nesta revisão.

Objetivo: remover `demo-ui/` e `landing/` do core, documentar o handoff para frontend separado e limpar a pipeline para validar apenas backend, Compose e segredos.

Entregáveis:

- `OpenAiEmbeddingProvider` real e opcional.
- Persistência de vetores em `document_chunks.embedding`.
- Recuperação pgvector quando consulta e chunks possuem embeddings.
- Fallback textual local quando não há chave, vetor ou resultado vetorial.
- Contrato HTTP para gateways externos.
- Documentação alinhada ao futuro `opiagile-whatsapp-ai-gateway`.

Pendências mantidas:

- Avaliação RAG automatizada.
- Demo pública com HTTPS.
- Autenticação e limites de uso.
- Frontend separado para demonstração visual.
- Extração de canal WhatsApp para aplicação gateway separada.
