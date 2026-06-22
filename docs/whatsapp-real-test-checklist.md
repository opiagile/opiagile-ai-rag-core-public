# Checklist Do Primeiro Teste Real WhatsApp

## Objetivo

Preparar o primeiro teste real pelo WhatsApp usando a Meta Cloud API direta, começando em dry-run e avançando para envio real somente depois que webhook, assinatura, allowlist, rate limit e resposta do `ChatService` estiverem validados.

Este checklist não contém segredos reais. Não copie tokens, app secret, verify token ou chaves de API para documentação, commits, issues ou mensagens de chat.

## Pré-requisitos Meta

- Meta App configurado.
- Produto WhatsApp adicionado ao app.
- Número de teste ou número business aprovado.
- Phone Number ID.
- WhatsApp Business Account ID.
- App Secret.
- Access Token.
- Verify Token escolhido pelo operador.
- Endpoint HTTPS público temporário ou deploy controlado.

## Variáveis Locais Esperadas

Configure no `.env` local, sem versionar o arquivo:

```text
WHATSAPP_PROVIDER=META_CLOUD
WHATSAPP_VERIFY_TOKEN=<nao registrar valor>
WHATSAPP_APP_SECRET=<nao registrar valor>
WHATSAPP_ACCESS_TOKEN=<nao registrar valor>
WHATSAPP_PHONE_NUMBER_ID=<nao registrar valor>
WHATSAPP_GRAPH_API_VERSION=v23.0
WHATSAPP_ALLOWED_TEST_NUMBERS=<apenas numeros autorizados>
WHATSAPP_PUBLIC_BASE_URL=<url https temporaria ou deploy controlado>
WHATSAPP_SEND_ENABLED=false
WHATSAPP_DRY_RUN=true
WHATSAPP_SIGNATURE_REQUIRED=true
WHATSAPP_RATE_LIMIT_PER_MINUTE=5
WHATSAPP_BLOCK_UNAUTHORIZED=true
```

Primeiro teste: `WHATSAPP_SEND_ENABLED=false` e `WHATSAPP_DRY_RUN=true`.

Envio real controlado, somente depois da validação em dry-run:

```text
WHATSAPP_SEND_ENABLED=true
WHATSAPP_DRY_RUN=false
```

## Ordem Do Teste

1. Subir banco e API local/controlada.
2. Confirmar que a API está saudável.
3. Rodar `scripts/check-whatsapp-pilot.sh` para validar configuração sem imprimir segredos.
4. Expor a API por HTTPS temporário ou deploy controlado.
5. Configurar callback URL na Meta:

```text
{PUBLIC_BASE_URL}/api/webhooks/whatsapp/meta
```

6. Configurar o verify token na Meta com o mesmo valor local.
7. Assinar eventos de mensagens no painel da Meta.
8. Validar o `GET /api/webhooks/whatsapp/meta` pelo painel da Meta.
9. O tester deve iniciar a conversa pelo próprio WhatsApp, enviando mensagem para o número de teste/business.
10. Confirmar evento recebido em dry-run.
11. Conferir `GET /api/webhooks/whatsapp/status`.
12. Conferir conversa, trace e logs seguros.
13. Validar que a resposta do `ChatService` está correta com LLM ou fallback aceitável.
14. Só então habilitar envio real para o número do responsável.
15. Depois de validar com o responsável, liberar no máximo 2 a 5 testers na allowlist.

## Critérios Para Ativar Envio Real

- Allowlist contém apenas o número do responsável no primeiro teste.
- Assinatura `X-Hub-Signature-256` está válida.
- Dry-run já foi testado com payload real da Meta.
- `ChatService` respondeu corretamente.
- LLM está validado ou fallback local está aceitável para a demonstração.
- `GET /api/webhooks/whatsapp/status` não expõe tokens, app secret, verify token ou lista completa de números.
- Logs não mostram dados sensíveis nem telefone completo quando não necessário.
- Tester iniciou a conversa pelo WhatsApp, evitando disparo fora de contexto.
- Operador entende que templates podem ser necessários para mensagens iniciadas pela empresa fora da janela de atendimento.

## HTTPS Temporário Ou Deploy Controlado

Opções aceitáveis:

- túnel temporário;
- deploy controlado;
- reverse proxy com HTTPS;
- ambiente cloud temporário.

Regras:

- não commitar URL efêmera como configuração fixa;
- não usar HTTPS público sem allowlist;
- não deixar túnel aberto desnecessariamente;
- não compartilhar URL com terceiros antes do teste do responsável;
- desligar túnel/deploy temporário quando o teste terminar.

## Controle De Custo E Abuso

- Começar sempre com `WHATSAPP_DRY_RUN=true`.
- Manter `WHATSAPP_SEND_ENABLED=false` até validar recebimento real.
- Manter allowlist mínima.
- Manter rate limit ativo.
- Monitorar uso OpenAI e Meta durante o teste.
- Não aceitar números fora da allowlist.

## Plano De Rollback

Se algo sair do esperado:

1. Definir `WHATSAPP_SEND_ENABLED=false`.
2. Definir `WHATSAPP_DRY_RUN=true`.
3. Remover número da allowlist.
4. Desligar túnel ou URL pública temporária.
5. Remover webhook no painel Meta, se necessário.
6. Revogar token caso haja suspeita de exposição.
7. Registrar o incidente sem copiar segredos.

## Resultado Esperado Do Primeiro Teste

- Mensagem real recebida pela API via Meta.
- Assinatura validada.
- Número reconhecido na allowlist.
- Rate limit respeitado.
- Conversa criada/atualizada com canal `WHATSAPP`.
- `ChatService` chamado.
- Dry-run registra que a resposta seria enviada, sem chamar Graph API.
- Envio real só é habilitado após validação explícita.
