# Caso De Uso: Suporte Técnico/SaaS

## Objetivo

Demonstrar triagem de suporte, resposta sobre SLA e escalonamento para incidentes críticos.

## Base De Conhecimento

Arquivo: `samples/suporte-tecnico/faq.txt`

Conteúdo coberto:

- horário de suporte;
- abertura de chamado;
- prazo de resposta;
- redefinição de senha;
- critérios de handoff humano.

## Perguntas De Teste

| Pergunta | Resultado esperado |
| --- | --- |
| Qual o prazo de resposta para chamado crítico? | Resposta com SLA de até 2 horas úteis. |
| Como faço para abrir um chamado? | Resposta pedindo nome, e-mail, empresa, produto e descrição. |
| Não consigo acessar meu e-mail cadastrado para redefinir senha | Handoff recomendado conforme regra do FAQ. |
| O sistema está totalmente indisponível | Handoff por indisponibilidade total. |

## Fluxo De Triagem

- Perguntas de SLA e senha entram como `DUVIDA_FAQ`.
- Abertura de chamado coleta dados do lead.
- Indisponibilidade total, problema financeiro, reclamação ou falha recorrente geram handoff.

## Demonstração Recomendada

1. Ingerir `samples/suporte-tecnico/faq.txt`.
2. Perguntar sobre prazo de chamado crítico.
3. Simular abertura de chamado com e-mail.
4. Simular indisponibilidade total e consultar handoffs.
