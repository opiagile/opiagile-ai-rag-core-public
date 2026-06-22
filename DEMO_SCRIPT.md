# Roteiro Executivo De Demonstração

## Objetivo

Mostrar que a Opiagile consegue construir um core RAG prático para produtos de atendimento com IA: ingestão de documentos, respostas com fontes, memória de conversa, triagem de leads, handoff humano, LLM opcional e contrato para gateways externos.

Este roteiro é executivo: serve para conduzir uma apresentação curta. O roteiro prático completo fica em [`docs/demo-script.md`](docs/demo-script.md).

## Posicionamento Correto

Use esta frase durante a demonstração:

> Core RAG com fontes, fallback textual local, pgvector com embeddings reais quando configurado e modo local sem chave externa.

Na v0.6, quando OpenAI está configurada para LLM e embeddings, o core pode gerar resposta conversacional e recuperar fontes por pgvector; sem chave externa, o fallback local continua funcionando.

Não apresente esta versão como ambiente produtivo ou acesso público aberto. O piloto WhatsApp Cloud API fica preservado como referência para futura extração em gateway separado.

## História Da Demonstração

Uma clínica fictícia possui um FAQ em `samples/clinica/faq.txt` com regras de agendamento, horários, política de cancelamento e documentos necessários. Um usuário faz perguntas, informa dados de lead, solicita agendamento e pede atendimento humano quando necessário.

## Preparação

```bash
cp .env.example .env
bash scripts/check-env.sh
docker compose --env-file .env up -d postgres
cd backend
./mvnw spring-boot:run
```


## Roteiro Sem Interface Gráfica

Este core agora deve ser demonstrado por API, scripts ou cliente HTTP. A interface visual foi removida do repositório e documentada em [`docs/frontend-handoff.md`](docs/frontend-handoff.md) para ser recriada em projeto separado.

## Fluxo De Apresentação

1. Validar que a API está viva:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/version
```

2. Enviar o documento de exemplo:

```bash
curl -F "file=@samples/clinica/faq.txt" http://localhost:8080/api/documents/upload
```

3. Fazer uma pergunta com RAG:

```text
Vocês atendem aos sábados?
```

Resultado esperado: resposta com fonte e referência ao documento indexado. Com LLM ativo, a resposta deve soar mais natural e sugerir próximo passo.

4. Demonstrar memória conversacional:

```text
Meu nome é João e quero agendar uma consulta.
E aos sábados?
```

Resultado esperado: a segunda mensagem reutiliza o contexto da conversa.

5. Demonstrar handoff humano:

```text
Quero falar com um atendente humano.
```

Resultado esperado: `handoffRequired=true` e registro disponível em `/api/handoffs`.

6. Demonstrar contrato para gateways externos:

```bash
curl -X POST http://localhost:8080/api/webhooks/whatsapp   -H 'Content-Type: application/json'   -d '{"provider":"MOCK","from":"+5511999999999","name":"João","message":"Quero agendar uma consulta","timestamp":"2026-05-28T10:00:00Z"}'
```

7. Demonstrar status do piloto WhatsApp preservado, se fizer sentido para explicar futura extração:

```bash
curl http://localhost:8080/api/webhooks/whatsapp/status
```

Resultado esperado: status operacional sem tokens, app secret, verify token ou lista completa de números.

8. Demonstrar observabilidade:

```bash
curl http://localhost:8080/api/observability/conversations/{conversationId}/trace
```

## Pontos De Venda Técnica

- O assistente responde com base em documentos enviados ao projeto.
- As fontes tornam a resposta auditável.
- A memória conversacional preserva contexto entre mensagens.
- Triagem de intenção e qualificação de leads conectam IA a operação real.
- Handoff evita insistir na automação quando uma pessoa deve assumir.
- Fluxos n8n mostram como a API se encaixa em processos de clientes.
- O modo DEMO roda sem chave externa; o modo LLM gera resposta mais natural quando `OPENAI_API_KEY` está configurada; embeddings reais ativam recuperação pgvector quando `OPENAI_EMBEDDINGS_ENABLED=true`; o piloto WhatsApp fica preservado como referência para gateway separado.
- Para apresentação visual a cliente leigo, crie um frontend separado usando `docs/frontend-handoff.md`.
