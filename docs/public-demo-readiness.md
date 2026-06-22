# Prontidão Para Demo Pública

## Objetivo

O projeto está pronto como portfólio técnico publicado, mas ainda não deve ser tratado como demo pública aberta para clientes testarem livremente.

Este documento separa o que já está pronto, o que falta para uma demo pública segura e a diferença entre demo pública e produção real.

## O Que Já Está Pronto

- Repositório público no GitHub.
- E2E local validado.
- Backend Java/Spring Boot.
- PostgreSQL/pgvector via Docker Compose.
- Upload TXT.
- Chunking.
- RAG com fontes, fallback textual local e pgvector quando embeddings estão configurados.
- Geração conversacional opcional com LLM quando OpenAI é configurada.
- Recuperação textual local no modo sem chave externa.
- Embeddings reais opcionais com OpenAI.
- Memória conversacional.
- Triagem de intenção.
- Qualificação de lead.
- Handoff humano.
- Piloto WhatsApp Cloud API preservado como referência para futura extração em gateway.
- Observabilidade por conversa.
- Workflows n8n de exemplo.
- Handoff de frontend separado documentado em `docs/frontend-handoff.md`.

## O Que Falta Para Demo Pública

- Deploy com HTTPS.
- Autenticação ou token simples de acesso.
- Rate limit para acesso público amplo.
- Reset de dados ou banco descartável.
- Seeds de demonstração.
- Ambiente separado do desenvolvimento local.
- Logs sem dados sensíveis.
- Proteção contra abuso de upload e chamadas repetidas.
- Garantia de que nenhuma chave real seja exposta.
- Política de retenção de dados.
- Aviso claro para não enviar dados sensíveis.

## Demo Pública Vs Produção Real

Demo pública:

- Ambiente controlado.
- Dados fictícios ou previamente aprovados.
- Limites de uso.
- Sem compromisso de SLA.
- Objetivo de avaliação comercial e técnica.

Produção real:

- Segurança revisada.
- Adequação à LGPD.
- Monitoramento contínuo.
- Backup e recuperação.
- Suporte operacional.
- Provedores reais configurados.
- Auditoria de logs.
- SLAs e plano de incidentes.

## Checklist De Prontidão

| Item | Status |
| --- | --- |
| Repositório público | Concluído |
| E2E local validado | Concluído |
| Backend e banco locais | Concluído |
| Frontend separado | Pendente |
| HTTPS público | Pendente |
| Autenticação/token | Pendente |
| Rate limit | Parcial: implementado para piloto WhatsApp; pendente para demo pública ampla |
| Contrato para UI upload/chat/fontes | Concluído em `docs/frontend-handoff.md` |
| UI pública com HTTPS | Pendente |
| Reset de dados | Pendente |
| Seeds de demonstração | Pendente |
| Política de retenção | Pendente |
| Assets finais | Parcial: roteiro e instruções criados, screenshots/GIF/vídeo pendentes |
| Embeddings reais opcionais | Concluído |
| Recuperação pgvector | Concluído quando chunks possuem embeddings |
| Gateway WhatsApp separado | Pendente |
| Envio real por canal externo | Fora do escopo deste core |


## Observação Sobre Frontend

A antiga `demo-ui/` foi removida deste core. O contrato para recriar a experiência visual em repositório separado está em `docs/frontend-handoff.md`. Para acesso externo ainda faltam HTTPS, autenticação ou token simples, rate limit geral, reset de dados, política de retenção e proteção contra abuso. O piloto WhatsApp preservado não equivale a uma demo pública aberta; envio real por canal externo deve ser tratado em aplicação gateway separada.
