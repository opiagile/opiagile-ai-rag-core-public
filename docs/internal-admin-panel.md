# Painel Interno De Administração

O painel interno de administração permite consultar solicitações do portal developers e aprovar a criação de API keys sandbox.

Ele foi criado para uso operacional restrito e não deve ser publicado em `opiagile.com`, `demo-rag.opiagile.com` ou qualquer outro domínio público.

## Modelo De Acesso

O painel é servido pelo Caddy apenas na porta `9090`, publicada no host como `127.0.0.1:9090`.

Isso significa que:

- não há acesso direto pela internet;
- a porta não fica aberta para visitantes externos;
- o acesso recomendado é por túnel SSH;
- os endpoints `/api/admin/*` continuam protegidos por `X-Demo-Admin-Token`.

## Como Acessar

Na sua máquina local, abra o túnel:

```bash
ssh -i ~/.ssh/sua_chave_oracle -L 9090:localhost:9090 ubuntu@<IP_PUBLICO_DA_VPS>
```

Com o túnel aberto, acesse:

```text
http://localhost:9090
```

Informe o token administrativo no campo `X-Demo-Admin-Token`.

## O Que O Painel Faz

- Lista solicitações developer por status.
- Preenche o formulário de aprovação a partir de uma solicitação.
- Aprova uma solicitação criando um sandbox temporário com tenant/workspace do cliente.
- Opcionalmente aprova uma solicitação em tenant/workspace já existente.
- Define escopos, limite por minuto e validade do sandbox.
- Exibe a API key gerada uma única vez.
- Gera link de uso único para entrega segura da API key ao lead por email.

## Sandbox Temporário

O modo recomendado para leads externos é `Criar sandbox temporário para este lead`.

Nesse modo:

- o backend cria um tenant e um workspace com nome derivado do cliente;
- a API key recebe `expires_at`;
- o tenant/workspace recebe `sandbox=true` e `expires_at`;
- a validade pode ser 24h, 48h ou 7 dias;
- após a expiração, o scheduler remove o tenant/workspace e os dados enviados naquele sandbox;
- a solicitação do lead permanece em `developer_access_requests` para contato, auditoria operacional e continuidade comercial.

Comunicação obrigatória para o lead:

```text
O sandbox é temporário. Ao expirar, tenant, workspace e dados enviados para teste são excluídos do ambiente sandbox. A solicitação de acesso permanece registrada para contato e acompanhamento comercial, conforme práticas de LGPD.
```

## Segurança

- O token administrativo não é embutido no HTML, CSS ou JavaScript.
- O armazenamento do token no navegador é opcional e local.
- A API key completa só aparece na resposta de aprovação e no link de entrega uma única vez.
- O link de entrega expira e é invalidado no primeiro acesso de revelação da chave.
- O banco armazena apenas prefixo e hash da API key.
- A chave usada para entrega por email fica criptografada temporariamente e é removida do fluxo útil após consumo/expiração.
- O Caddy público não roteia `/api/admin/*`.
- A porta `9090` é publicada apenas em `127.0.0.1`.
- O único email visível para leads e usuários é `contato@opiagile.com`.

## Operação Recomendada

1. Abrir túnel SSH.
2. Acessar `http://localhost:9090`.
3. Informar o token administrativo.
4. Carregar solicitações novas.
5. Aprovar somente solicitações validadas.
6. Preferir sandbox temporário para leads externos.
7. Conferir a API key, tenant/workspace, expiração e link de entrega gerados.
8. Confirmar que o scheduler enviou o email ao lead com link de uso único.
9. Usar cópia manual apenas como contingência operacional.
10. Fechar a janela e encerrar o túnel SSH.

## Limitações

- Ainda não há login de usuário administrativo.
- Ainda não há rejeição formal de solicitações.
- Ainda não há revogação visual de API keys.
- A entrega por email depende de SMTP válido e permissão para enviar como `contato@opiagile.com`.
