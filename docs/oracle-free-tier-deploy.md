# Deploy Na Oracle Cloud Always Free

Guia para subir o `opiagile-ai-rag-core` em uma VPS Oracle Cloud Always Free com Ubuntu 24.04 ARM64.

Este guia não assume acesso SSH por agente. Todos os comandos abaixo são para execução manual na VPS.

## Dados Da VPS

```text
IP público: 136.248.83.176
Usuário SSH: ubuntu
Sistema: Ubuntu 24.04 ARM / aarch64
Shape: Oracle VM.Standard.A1.Flex
CPU: 2 OCPUs
RAM: 12 GB
Disco: 200 GB
Arquitetura: ARM64
Chave SSH local: ~/.ssh/opiagile_oracle_cloud
```

Acesso:

```bash
ssh -i ~/.ssh/opiagile_oracle_cloud ubuntu@136.248.83.176
```

## 1. Checklist De Pré-Requisitos

Antes de iniciar:

- A VPS precisa estar acessível por SSH.
- A regra de entrada da Oracle Cloud precisa permitir TCP `22`.
- Para web/HTTPS, a Security List ou NSG da Oracle também deve permitir TCP `80` e `443`.
- Se for usar HTTPS automático, o domínio ou subdomínio deve apontar para `136.248.83.176`.
- Não exponha PostgreSQL na internet.
- Não rode LLM pesado local na VPS. Use APIs externas para LLM e embeddings.
- Tenha uma chave OpenAI somente se quiser ativar LLM/embeddings reais.

Referências oficiais usadas:

- Docker Engine no Ubuntu suporta Ubuntu 24.04 e arquitetura `arm64`.
- A imagem `pgvector/pgvector` publica variantes Linux ARM64.
- GitHub Actions deve receber secrets por `Settings > Secrets and variables > Actions`.

## 2. Atualizar Ubuntu

Execute na VPS:

```bash
sudo apt update
sudo apt -y upgrade
sudo apt -y install ca-certificates curl gnupg git ufw unzip jq
sudo reboot
```

Reconecte após o reboot:

```bash
ssh -i ~/.ssh/opiagile_oracle_cloud ubuntu@136.248.83.176
```

Confirme arquitetura:

```bash
uname -m
```

Esperado:

```text
aarch64
```

## 3. Instalar Docker E Docker Compose

Remova pacotes conflitantes, se existirem:

```bash
for pkg in docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc; do
  sudo apt-get remove -y "$pkg" || true
done
```

Configure o repositório oficial do Docker:

```bash
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

sudo apt update
```

Instale Docker Engine e o plugin Compose:

```bash
sudo apt -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
```

Permita que o usuário `ubuntu` use Docker:

```bash
sudo usermod -aG docker ubuntu
```

Saia e reconecte:

```bash
exit
ssh -i ~/.ssh/opiagile_oracle_cloud ubuntu@136.248.83.176
```

Valide:

```bash
docker --version
docker compose version
docker run --rm hello-world
```

## 4. Estrutura De Diretórios Recomendada

```bash
sudo mkdir -p /opt/opiagile
sudo chown -R ubuntu:ubuntu /opt/opiagile
cd /opt/opiagile
```

Clone a branch `develop`:

```bash
git clone -b develop https://github.com/opiagile/opiagile-ai-rag-core.git opiagile-ai-rag-core
cd /opt/opiagile/opiagile-ai-rag-core
```

Se a branch `develop` ainda não existir no remoto, crie a partir da sua máquina local ou use temporariamente `main`:

```bash
git clone -b main https://github.com/opiagile/opiagile-ai-rag-core.git opiagile-ai-rag-core
```

## 5. Arquivos De Deploy

Os arquivos ficam em:

```text
deploy/oracle-free-tier/
├── Dockerfile.api
├── docker-compose.yml
├── Caddyfile.http
├── Caddyfile.https
├── .env.production.example
└── scripts/
    ├── update.sh
    ├── backup-db.sh
    └── restore-db.sh
```

Copie o exemplo de ambiente:

```bash
cd /opt/opiagile/opiagile-ai-rag-core/deploy/oracle-free-tier
cp .env.production.example .env
chmod 600 .env
```

Edite:

```bash
nano .env
```

Troque obrigatoriamente:

```text
POSTGRES_PASSWORD=troque_esta_senha
```

Para primeiro teste sem custo externo, deixe:

```text
LLM_PROVIDER=DEMO
CHAT_RESPONSE_MODE=DEMO
OPENAI_API_KEY=
OPENAI_EMBEDDINGS_ENABLED=false
```

Para testar LLM e embeddings reais:

```text
LLM_PROVIDER=OPENAI
CHAT_RESPONSE_MODE=LLM
OPENAI_API_KEY=<sua_chave_fora_do_git>
OPENAI_EMBEDDINGS_ENABLED=true
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
OPENAI_EMBEDDING_DIMENSIONS=1536
```

Observação: APIs externas podem gerar custo fora da Oracle Free Tier.

## 6. PostgreSQL Com pgvector

O Compose usa:

```text
pgvector/pgvector:pg16-trixie
```

O banco fica somente na rede Docker interna e não publica porta no host.

Flyway cria:

- extensão `vector`;
- tabelas de documentos, chunks, conversas, handoff e logs;
- coluna `document_chunks.embedding vector(1536)`.

## 7. Redis

Redis não é necessário para o estado atual do `opiagile-ai-rag-core`.

Não instale Redis agora. Isso reduz consumo de RAM, superfície de ataque e manutenção.

## 8. Proxy Reverso Com Caddy

O deploy usa Caddy por simplicidade:

- sem domínio: HTTP em `http://136.248.83.176`;
- com domínio: HTTPS automático com Let's Encrypt.

### Sem Domínio

No `.env`:

```text
CADDYFILE_PATH=./Caddyfile.http
RAG_DOMAIN=
ACME_EMAIL=
```

### Com Domínio

Antes, aponte um registro DNS `A` para:

```text
136.248.83.176
```

Exemplo:

```text
rag.opiagile.com.br -> 136.248.83.176
```

No `.env`:

```text
CADDYFILE_PATH=./Caddyfile.https
RAG_DOMAIN=rag.seudominio.com.br
ACME_EMAIL=seu-email@dominio.com.br
```

Caddy expõe somente `80` e `443` e encaminha internamente para `api:8080`.

## 9. Firewall UFW

Ative regras mínimas no Ubuntu:

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw enable
sudo ufw status verbose
```

Na Oracle Cloud, confirme também a liberação de ingress:

- TCP `22`;
- TCP `80`;
- TCP `443`.

Não libere `5432`.

## 10. Subir Aplicação

```bash
cd /opt/opiagile/opiagile-ai-rag-core/deploy/oracle-free-tier
docker compose --env-file .env config
docker compose --env-file .env up -d --build
docker compose --env-file .env ps
```

Ver logs:

```bash
docker compose --env-file .env logs -f api
docker compose --env-file .env logs -f postgres
docker compose --env-file .env logs -f caddy
```

Parar:

```bash
docker compose --env-file .env down
```

Subir novamente:

```bash
docker compose --env-file .env up -d
```

Atualizar código manualmente:

```bash
cd /opt/opiagile/opiagile-ai-rag-core
git fetch origin develop
git checkout develop
git pull --ff-only origin develop
cd deploy/oracle-free-tier
docker compose --env-file .env up -d --build
```

Ou use o script:

```bash
cd /opt/opiagile/opiagile-ai-rag-core
bash deploy/oracle-free-tier/scripts/update.sh
```

## 11. Backup Do Banco

Backup manual:

```bash
cd /opt/opiagile/opiagile-ai-rag-core
bash deploy/oracle-free-tier/scripts/backup-db.sh
```

Backups ficam em:

```text
deploy/oracle-free-tier/backups/
```

Agendar backup diário às 03:20 UTC:

```bash
crontab -e
```

Adicionar:

```cron
20 3 * * * cd /opt/opiagile/opiagile-ai-rag-core && bash deploy/oracle-free-tier/scripts/backup-db.sh >> /opt/opiagile/backup-rag.log 2>&1
```

Restore:

```bash
cd /opt/opiagile/opiagile-ai-rag-core
bash deploy/oracle-free-tier/scripts/restore-db.sh deploy/oracle-free-tier/backups/postgres-YYYYMMDDTHHMMSSZ.dump
```

Copie backups importantes para fora da VPS periodicamente.

## 12. Validar Funcionamento

Health:

```bash
curl -i http://localhost:8080/actuator/health
curl -i http://136.248.83.176/actuator/health
```

Versão:

```bash
curl -s http://136.248.83.176/api/version | jq
```

Upload de documento:

```bash
cd /opt/opiagile/opiagile-ai-rag-core
curl -F "file=@samples/clinica/faq.txt" http://136.248.83.176/api/documents/upload | jq
```

Upload acima do limite da demo deve retornar erro controlado:

```bash
python3 - <<'PY'
from pathlib import Path
Path('/tmp/rag-arquivo-grande.txt').write_text('a' * 270000)
PY
curl -i -F "file=@/tmp/rag-arquivo-grande.txt" http://136.248.83.176/api/documents/upload
```

Limites controlados por `.env` no deploy:

```text
DOCUMENT_UPLOAD_MAX_BYTES=262144
DOCUMENT_UPLOAD_MAX_CHARS=200000
DOCUMENT_UPLOAD_MAX_CHUNKS=300
DOCUMENT_UPLOAD_MAX_DOCUMENTS=50
SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=256KB
SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=300KB
CADDY_REQUEST_BODY_MAX_SIZE=300KB
```

Proteção opcional para demo exposta:

```text
DEMO_ACCESS_TOKEN=
DEMO_ADMIN_TOKEN=
DEMO_RATE_LIMIT_ENABLED=true
DEMO_CHAT_RATE_LIMIT_PER_MINUTE=30
DEMO_UPLOAD_RATE_LIMIT_PER_MINUTE=5
```

Se `DEMO_ACCESS_TOKEN` estiver preenchido, chamadas de escrita devem enviar:

```bash
-H "X-Demo-Token: TOKEN_DEMO_LOCAL"
```

Reset protegido de dados da demo, apenas quando `DEMO_ADMIN_TOKEN` estiver preenchido:

```bash
curl -X POST http://136.248.83.176/api/admin/demo/reset \
  -H "X-Demo-Admin-Token: TOKEN_ADMIN_LOCAL" | jq
```

Chat:

```bash
curl -s http://136.248.83.176/api/chat \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo" \
  -H "X-Workspace-Id: clinica-demo" \
  -d '{
    "message": "Vocês atendem aos sábados?",
    "channel": "WEB",
    "contactId": "oracle-test"
  }' | jq
```

Com domínio:

```bash
curl -i https://rag.seudominio.com.br/actuator/health
```

## 13. Diagnóstico De Problemas Comuns

### Docker sem permissão

Sintoma:

```text
permission denied while trying to connect to the Docker daemon socket
```

Correção:

```bash
sudo usermod -aG docker ubuntu
exit
ssh -i ~/.ssh/opiagile_oracle_cloud ubuntu@136.248.83.176
```

### API não sobe

```bash
cd /opt/opiagile/opiagile-ai-rag-core/deploy/oracle-free-tier
docker compose --env-file .env logs --tail=200 api
docker compose --env-file .env ps
```

Verifique:

- `POSTGRES_PASSWORD` preenchido;
- Postgres saudável;
- migrations Flyway sem erro;
- memória livre com `free -h`.

### Banco não está saudável

```bash
docker compose --env-file .env logs --tail=200 postgres
docker volume ls
df -h
```

### Caddy não emite HTTPS

Verifique:

```bash
dig +short rag.seudominio.com.br
curl -I http://rag.seudominio.com.br
docker compose --env-file .env logs --tail=200 caddy
```

Confirme:

- domínio aponta para `136.248.83.176`;
- portas `80` e `443` liberadas no UFW;
- portas `80` e `443` liberadas na Oracle Cloud;
- `CADDYFILE_PATH=./Caddyfile.https`;
- `RAG_DOMAIN` e `ACME_EMAIL` preenchidos.

### OpenAI não responde ou cai em DEMO

Verifique sem imprimir chave:

```bash
grep -E '^(LLM_PROVIDER|CHAT_RESPONSE_MODE|OPENAI_EMBEDDINGS_ENABLED|OPENAI_CHAT_MODEL|OPENAI_EMBEDDING_MODEL)=' deploy/oracle-free-tier/.env
test -n "$(grep '^OPENAI_API_KEY=' deploy/oracle-free-tier/.env | cut -d= -f2-)" && echo "OPENAI_API_KEY presente" || echo "OPENAI_API_KEY ausente"
```

Reinicie após alterar `.env`:

```bash
docker compose --env-file .env up -d --build
```

### pgvector não recupera semanticamente

Lembre:

- embeddings só são gravados em documentos ingeridos depois de `OPENAI_EMBEDDINGS_ENABLED=true`;
- documentos antigos sem vetor usam fallback textual;
- reenvie o arquivo depois de habilitar embeddings.

## 14. GitHub Actions Para develop

O workflow `.github/workflows/develop-ci-deploy.yml` valida pushes e PRs para `develop`.

Ele só faz deploy quando:

- o evento é `push` na branch `develop`;
- a variável do repositório `ORACLE_DEPLOY_ENABLED` está como `true`;
- os secrets de SSH estão configurados.

Secrets sugeridos:

```text
ORACLE_HOST=136.248.83.176
ORACLE_USER=ubuntu
ORACLE_SSH_KEY=<conteúdo da chave privada de deploy>
ORACLE_SSH_PORT=22
ORACLE_DEPLOY_PATH=/opt/opiagile/opiagile-ai-rag-core
```

Variable:

```text
ORACLE_DEPLOY_ENABLED=true
```

Na VPS, o arquivo `.env` de produção continua manual e não deve ser versionado.

## 15. Próximos Passos Recomendados

1. Subir primeiro em HTTP pelo IP.
2. Validar upload e chat em modo DEMO.
3. Ativar domínio e HTTPS.
4. Ativar LLM/embeddings com `OPENAI_API_KEY`, se quiser testar RAG real.
5. Criar branch `develop` e proteger o deploy por GitHub Actions.
6. Só depois avaliar demo pública com autenticação/rate limit.
