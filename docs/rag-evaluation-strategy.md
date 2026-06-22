# Estratégia De Avaliação RAG

## Objetivo

Este documento define como avaliar o comportamento de RAG do `opiagile-ai-rag-core`.

O projeto possui RAG com fontes, fallback textual local e recuperação pgvector quando embeddings reais estão habilitados.

Frase de posicionamento:

> Core RAG com fontes, fallback textual local, pgvector com embeddings reais quando configurado e modo local sem chave externa.

## Estado Atual

- Upload TXT por `POST /api/documents/upload`.
- Chunking de documentos.
- Persistência em `documents` e `document_chunks`.
- Recuperação textual determinística no modo sem chave externa.
- Embeddings reais opcionais com OpenAI quando `OPENAI_EMBEDDINGS_ENABLED=true`.
- Recuperação pgvector quando a consulta e os chunks possuem embeddings.
- Fallback textual local quando embeddings não estão disponíveis ou não retornam fontes.
- Respostas com fontes.
- Logs de recuperação em `retrieval_logs`.
- Trace por conversa em `/api/observability/conversations/{id}/trace`.
- Execução local sem `OPENAI_API_KEY`.

## Como Avaliar Respostas RAG

Critérios recomendados:

- Resposta correta para a pergunta feita.
- Presença de fonte na resposta da API.
- Aderência ao documento recuperado.
- Ausência de informação inventada.
- Uso adequado do contexto conversacional.
- Handoff quando a resposta não for segura, estiver fora do escopo ou o usuário pedir humano.
- Clareza para usuário final.

## Score Sugerido De 0 A 5

| Score | Critério |
| --- | --- |
| 0 | Resposta errada e sem fonte. |
| 1 | Resposta parcialmente relacionada, sem fonte útil. |
| 2 | Resposta incompleta, com fonte fraca ou pouco aderente. |
| 3 | Resposta aceitável, com fonte. |
| 4 | Resposta correta, clara e com boa fonte. |
| 5 | Resposta correta, clara, com boa fonte e comportamento operacional adequado, incluindo handoff quando necessário. |

## Perguntas Mínimas Por Nicho

### Clínica

- Vocês atendem aos sábados?
- Quais documentos preciso levar?
- Como faço para remarcar uma consulta?
- Quero cancelar minha consulta.
- Quero falar com um atendente humano.

### Imobiliária

- Quais documentos preciso para alugar um imóvel?
- Posso agendar uma visita?
- Tenho interesse em comprar um apartamento.
- Quero falar com um corretor.
- Quais informações preciso enviar para análise?

### Suporte Técnico/SaaS

- Como faço para redefinir minha senha?
- O sistema está fora do ar, o que devo fazer?
- Qual o prazo de atendimento?
- Quero abrir um chamado urgente.
- Preciso falar com suporte humano.


## Critério De Fluidez Conversacional

A v0.4 adiciona resposta conversacional opcional com LLM. No modo DEMO, a resposta continua determinística para rodar sem chave externa. No modo LLM, a avaliação deve medir se a resposta parece atendimento real sem perder aderência às fontes.

Score sugerido:

- 0: resposta técnica, errada ou robótica.
- 1: resposta relacionada, mas pouco natural.
- 2: resposta correta, mas seca e sem próximo passo.
- 3: resposta correta e clara.
- 4: resposta correta, clara, educada e com próximo passo útil.
- 5: resposta correta, fluida, aderente às fontes, com contexto e comportamento operacional adequado.

Comparação esperada:

- Antes: resposta determinística citando o trecho recuperado de forma mais rígida.
- Com LLM: resposta curta, natural, educada, baseada no trecho recuperado e com sugestão de próximo passo.

## Caminho Para Avaliação Automatizada

Próximos incrementos técnicos:

1. Criar dataset com perguntas, respostas esperadas e fontes esperadas.
2. Comparar recuperação textual local com recuperação pgvector.
3. Registrar separadamente provider de recuperação, score, fonte esperada e fonte obtida.
4. Medir precisão das fontes recuperadas por nicho.
5. Medir fluidez da resposta com LLM sem relaxar aderência às fontes.
6. Automatizar avaliação RAG em CI ou script local.
7. Documentar limitações por modelo, idioma, qualidade do documento e custo de embeddings.
