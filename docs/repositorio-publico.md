# Repositório público

Este repositório é um snapshot público do core RAG da Opiagile para portfólio técnico.

Ele demonstra arquitetura, testes, segurança básica, contrato HTTP, isolamento por workspace, recuperação com fontes, LLM opcional, pgvector e preparação para integrações externas.

Por segurança, este snapshot público não inclui:

- arquivos `.env`;
- chaves, tokens ou credenciais reais;
- chave privada SSH;
- workflows de deploy real;
- workflows que consomem chaves reais de provedores externos;
- dados sensíveis de clientes.

Para rodar localmente, use `.env.example` como base e configure suas próprias variáveis em `.env`, sem commitar esse arquivo.
