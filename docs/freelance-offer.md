# Oferta Freelancer

## Oferta Principal

Implantação de core RAG com IA baseado nos documentos do cliente, com respostas com fontes, embeddings/pgvector quando configurados, triagem de intenção, memória de conversa e handoff humano. Integrações reais com canais como WhatsApp, Teams ou CRM devem ser feitas em gateways separados, conectados ao core por contrato HTTP.

## Entregáveis Possíveis

- API core de RAG.
- Ingestão de documentos.
- Base de perguntas frequentes.
- Contrato HTTP para gateways externos.
- Fluxos n8n.
- Painel ou relatório de handoffs.
- Observabilidade básica de respostas.

## Escopo Inicial Sugerido

1. Mapear documentos e perguntas frequentes.
2. Configurar ingestão e busca RAG.
3. Criar fluxo de atendimento e handoff.
4. Integrar canal de entrada por gateway separado, se fizer parte do escopo.
5. Validar respostas com perguntas reais.

## Limites Claros

- O assistente não substitui atendimento humano em casos críticos.
- Respostas devem ser avaliadas com dados reais do cliente.
- Integrações de canal dependem do provedor escolhido, HTTPS público, credenciais, regras da plataforma, consentimento e allowlist inicial.
