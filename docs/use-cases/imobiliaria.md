# Caso De Uso: Imobiliária

## Objetivo

Demonstrar atendimento inicial para compra, aluguel, anúncio de imóvel e agendamento de visitas.

## Base De Conhecimento

Arquivo: `samples/imobiliaria/faq.txt`

Conteúdo coberto:

- horário de atendimento;
- qualificação de interesse em imóvel;
- agendamento de visita;
- documentos para locação;
- critérios de handoff humano.

## Perguntas De Teste

| Pergunta | Resultado esperado |
| --- | --- |
| Vocês fazem visitas aos sábados? | Resposta com horário de sábado para visitas agendadas. |
| Quais documentos preciso para locação? | Resposta com documento, renda, residência e análise cadastral. |
| Meu nome é Carla e quero alugar um apartamento de dois quartos | Intenção comercial/agendamento conforme texto e lead com interesse salvo. |
| Quero negociar o valor com um corretor | Encaminhamento para humano por negociação/corretor. |

## Fluxo De Triagem

- Compra, aluguel e anúncio alimentam interesse do lead.
- Visita agenda follow-up comercial.
- Proposta, negociação, urgência contratual ou pedido de corretor geram handoff.

## Demonstração Recomendada

1. Ingerir `samples/imobiliaria/faq.txt`.
2. Consultar documentos de locação.
3. Simular lead de aluguel com telefone.
4. Pedir corretor humano e verificar `/api/handoffs`.
