# Roteiro De Vídeo Do Core RAG

## Objetivo

Criar um vídeo curto, de 60 a 90 segundos, mostrando o `opiagile-ai-rag-core` como backend/API de RAG. A demonstração deve usar terminal, cliente HTTP ou um frontend separado criado a partir de `docs/frontend-handoff.md`.

## Mensagem Central

> Este é o core RAG da Opiagile: uma API para ingerir documentos, gerar embeddings, recuperar fontes, responder com LLM opcional e expor contrato para frontends e gateways.

## Cenas Sugeridas

1. Mostrar README e escopo do core.
2. Validar `/actuator/health` e `/api/version`.
3. Enviar `samples/clinica/faq.txt`.
4. Fazer pergunta em `/api/chat`.
5. Mostrar `answer`, `sources`, `responseMode`, `llmProvider` e `conversationId`.
6. Fazer pergunta de memória com mesmo `conversationId`.
7. Acionar handoff com `Quero falar com um atendente humano.`
8. Consultar trace da conversa.
9. Fechar mostrando `docs/frontend-handoff.md` e `docs/gateway-contract.md`.

## Narração Sugerida

```text
Este projeto é o core RAG da Opiagile. Ele recebe documentos, quebra em chunks, gera embeddings quando OpenAI está configurada, recupera fontes por pgvector e gera respostas com LLM opcional.

O core também mantém memória conversacional, identifica intenção, registra handoff e expõe rastros para auditoria.

Interfaces visuais e canais como WhatsApp devem consumir essa API em projetos separados, mantendo o core focado na inteligência de RAG.
```

## Legenda Para Divulgação

```text
Core RAG da Opiagile: upload de documentos, embeddings opcionais, pgvector, respostas com fontes, LLM opcional, memória conversacional, handoff e contrato para frontends/gateways separados.
```

## Cuidados

- Não mostrar `.env`.
- Não mostrar chaves OpenAI ou tokens de gateway.
- Usar dados fictícios.
- Não apresentar como produção.
- Não prometer demo pública aberta.
