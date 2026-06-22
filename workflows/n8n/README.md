# Exemplos De Fluxos n8n

Esta pasta contém fluxos de demonstração para mostrar como a API da Opiagile pode entrar em automações de atendimento. Os arquivos não exigem credenciais reais e usam a API local como destino.

## Variáveis

```text
OPIAGILE_API_URL=http://host.docker.internal:8080
```

No Linux, se `host.docker.internal` não estiver disponível no ambiente de execução do n8n, use o IP do host ou coloque o n8n na mesma rede Docker do backend.

## Workflows

### `whatsapp-triage-demo.json`

Recebe um payload estilo WhatsApp, chama `POST /api/webhooks/whatsapp` e roteia a resposta conforme `handoffRequired`.

Payload de teste:

```json
{
  "provider": "MOCK",
  "from": "+5511999999999",
  "name": "João",
  "message": "Quero agendar uma consulta",
  "timestamp": "2026-05-28T08:53:00-04:00"
}
```

### `rag-faq-demo.json`

Recebe uma pergunta simples, chama `POST /api/chat` e devolve resposta, fontes e `conversationId`.

Payload de teste:

```json
{
  "message": "Vocês atendem aos sábados?",
  "contactId": "faq-demo"
}
```

### `human-handoff-demo.json`

Consulta `GET /api/handoffs`, separa registros abertos e prepara um card com motivo, resumo e identificador do handoff. Em uma implantação validada com o cliente, o último node pode ser trocado por Slack, e-mail, CRM ou Chatwoot.

## Observações

- Os JSONs são exemplos de portfólio e podem precisar de pequenos ajustes conforme a versão local do n8n.
- Nenhum fluxo contém token, segredo ou URL real de cliente.
- A intenção é demonstrar desenho de automação, não substituir uma implantação real com segurança, credenciais e monitoramento.
