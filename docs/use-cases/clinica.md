# Caso De Uso: Clínica/Consultório

## Objetivo

Demonstrar um assistente que responde dúvidas frequentes de atendimento, orienta agendamento e aciona humano em situações sensíveis.

## Base De Conhecimento

Arquivo: `samples/clinica/faq.txt`

Conteúdo coberto:

- horário de atendimento;
- regras de agendamento, remarcação e cancelamento;
- documentos necessários;
- critérios de handoff humano.

## Perguntas De Teste

| Pergunta | Resultado esperado |
| --- | --- |
| Vocês atendem aos sábados? | Resposta com horário de sábado e fontes do FAQ. |
| Quais documentos preciso levar? | Resposta citando documento com foto, carteirinha, exames e pedido médico. |
| Meu nome é João e quero agendar uma consulta sexta de manhã | Intenção `AGENDAR`, lead qualificado e fonte de agendamento. |
| Quero falar com um atendente humano | `handoffRequired=true` e criação de handoff. |

## Fluxo De Triagem

- Dúvidas comuns entram como `DUVIDA_FAQ`.
- Pedidos de agendamento entram como `AGENDAR`.
- Reclamação, urgência médica ou pedido de humano geram `NEEDS_HUMAN`.

## Demonstração Recomendada

1. Subir PostgreSQL e backend.
2. Enviar `samples/clinica/faq.txt` em `/api/documents/upload`.
3. Perguntar sobre sábado em `/api/chat`.
4. Enviar uma mensagem de agendamento com nome e telefone.
5. Enviar pedido de humano e abrir `/api/handoffs`.
