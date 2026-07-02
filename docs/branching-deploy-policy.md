# Política De Branches E Deploy

Este repositório usa duas linhas principais:

- `main`: linha estável do core RAG. Deve representar o estado aprovado para versionamento, referência pública e evolução planejada.
- `develop`: linha de demonstração controlada. Todo push em `develop` aciona a pipeline de validação e deploy na VPS Oracle, quando `ORACLE_DEPLOY_ENABLED=true`.

## Regra Operacional

| Branch | Uso | Deploy automático |
| --- | --- | --- |
| `main` | Versão estável e referência do projeto | Não |
| `develop` | Ambiente Oracle/demo e validação integrada | Sim |
| `feature/*` | Mudanças maiores ou arriscadas | Não |
| `fix/*` | Correções pontuais | Não |

Mudanças pequenas podem ser feitas diretamente em `develop` quando o risco for baixo. Mudanças maiores devem sair de uma branch curta e entrar em `develop` por merge ou PR.

Depois de validado na VPS, `develop` pode ser promovida para `main`.

## Fluxo Recomendado

1. Criar ou atualizar código em `develop` ou branch curta.
2. Rodar validações locais.
3. Fazer push para `develop`.
4. Acompanhar o workflow `Validar e publicar develop na Oracle`.
5. Validar endpoints públicos no ambiente Oracle.
6. Se o resultado estiver estável, promover `develop` para `main`.

## Checklist De Validação Antes De Promover Para main

Use este checklist depois que `develop` foi publicado na Oracle:

- [ ] Workflow `Validar e publicar develop na Oracle` passou.
- [ ] `GET /actuator/health` retornou `UP`.
- [ ] `GET /api/version` mostra a versão Java/runtime esperada.
- [ ] `GET /api/providers/status` responde corretamente com credencial autorizada e não expõe segredo.
- [ ] Portal `/developers` abre.
- [ ] Console `/developers/console` abre.
- [ ] OpenAPI `/v3/api-docs/rag-core` responde.
- [ ] Upload de documento foi validado no ambiente de demo, quando houver chave/token apropriado.
- [ ] Chat RAG foi validado no ambiente de demo, quando houver chave/token apropriado.
- [ ] Embeddings foram validados quando `OPENAI_EMBEDDINGS_ENABLED=true`.
- [ ] Nenhum segredo foi versionado.
- [ ] Documentação e relatório da sprint estão atualizados.

## Promoção De develop Para main

Quando a `develop` estiver validada:

```bash
git checkout main
git pull --ff-only origin main
git merge --no-ff develop -m "chore: promove develop validada"
git push origin main
```

Se houver conflito, resolver manualmente e repetir as validações relevantes antes do push.

## Rollback Operacional Da VPS

Se um deploy em `develop` causar problema:

1. Identificar o último commit saudável em `develop`.
2. Fazer revert do commit problemático ou aplicar correção nova em `develop`.
3. Fazer push em `develop` para disparar novo deploy.
4. Validar `/actuator/health` e `/api/version`.

Evite reescrever histórico de `develop` depois que a pipeline já publicou o commit.

## Observações De Segurança

- O deploy automático nunca deve depender de `.env` versionado.
- Secrets de GitHub Actions devem ficar em `secrets` ou `vars`, nunca no repositório.
- A VPS mantém `.env` operacional fora do Git.
- Logs e relatórios não devem imprimir `OPENAI_API_KEY`, API keys de clients, chaves SSH, tokens Meta ou senhas.
