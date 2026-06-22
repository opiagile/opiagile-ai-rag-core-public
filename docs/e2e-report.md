# Relatório E2E

Data da execução: 2026-05-28
Ambiente: Linux ARM64/aarch64 em VM Parallels
API testada: `http://localhost:8080`

## Resultado

O teste E2E passou com sucesso cobrindo os principais fluxos implementados no projeto.

## Preparação Executada

- PostgreSQL/pgvector confirmado com `docker compose --env-file .env up -d postgres`.
- API Spring Boot iniciada com `cd backend && ./mvnw spring-boot:run`.
- Script E2E executado com `scripts/e2e-demo.sh`.
- API temporária parada após a execução.

## Cobertura Do E2E

- Health check em `/actuator/health`.
- Versão da aplicação em `/api/version`.
- Upload TXT em `/api/documents/upload`.
- Listagem, detalhe e chunks de documentos.
- Chat RAG em `/api/chat` com fontes.
- Memória conversacional usando o mesmo `conversationId`.
- Histórico de mensagens e resumo da conversa.
- Triagem de intenção com intenção `AGENDAR`.
- Pedido explícito de humano com `handoffRequired=true`.
- Listagem, detalhe e atualização de status de handoff.
- Webhook WhatsApp mock em `/api/webhooks/whatsapp`.
- Trace de observabilidade em `/api/observability/conversations/{id}/trace`.
- Verificação dos textos principais da landing institucional.

## Saída Resumida

```text
[ok] health da API está UP
[ok] version respondeu 0.1.0-SNAPSHOT
[ok] documento enviado com 2 chunks
[ok] listagem de documentos contém o upload do E2E
[ok] detalhe do documento respondeu corretamente
[ok] chunks do documento foram retornados
[ok] chat RAG respondeu com fontes e intenção AGENDAR
[ok] segunda mensagem reutilizou a conversa existente
[ok] histórico de conversa foi persistido
[ok] resumo da conversa foi consultado
[ok] pedido de humano acionou handoff
[ok] handoff apareceu na listagem
[ok] detalhe do handoff respondeu corretamente
[ok] status do handoff foi atualizado
[ok] webhook WhatsApp mock respondeu e criou conversa
[ok] observabilidade retornou mensagens e recuperações
[ok] landing contém os textos principais
E2E concluído com sucesso.
```

## Script Reutilizável

O roteiro automatizado ficou em:

```bash
scripts/e2e-demo.sh
```

Ele usa Python padrão e aceita `API_BASE_URL` para testar outro host:

```bash
API_BASE_URL=http://localhost:8080 scripts/e2e-demo.sh
```

## Observações

- O E2E usa dados reais no banco local de desenvolvimento e cria novos documentos, conversas, mensagens, leads, handoffs e logs de recuperação a cada execução.
- O fluxo foi validado em modo demonstração, sem chave OpenAI e sem credenciais reais de WhatsApp.
- O teste de recuperação semântica real com pgvector/embeddings segue pendente até configurar `OPENAI_API_KEY` ou provedor equivalente.
