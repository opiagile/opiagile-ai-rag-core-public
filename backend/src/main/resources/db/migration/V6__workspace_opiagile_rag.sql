INSERT INTO tenants (slug, name)
VALUES ('opiagile', 'Opiagile')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO workspaces (tenant_id, slug, name, description)
SELECT t.id,
       'opiagile-rag',
       'Assistente Opiagile',
       'Base de conhecimento para atender leads e explicar serviços, demo RAG, integrações, segurança e LGPD.'
FROM tenants t
WHERE t.slug = 'opiagile'
ON CONFLICT (tenant_id, slug) DO NOTHING;

WITH contexto AS (
    SELECT t.id AS tenant_id, w.id AS workspace_id
    FROM tenants t
    JOIN workspaces w ON w.tenant_id = t.id
    WHERE t.slug = 'opiagile'
      AND w.slug = 'opiagile-rag'
),
documento AS (
    INSERT INTO documents (tenant_id, workspace_id, filename, content_type, source_type, status)
    SELECT tenant_id, workspace_id, 'opiagile-rag-conhecimento.md', 'text/markdown', 'SEED', 'INDEXED'
    FROM contexto
    RETURNING id
)
INSERT INTO document_chunks (document_id, chunk_index, content, metadata)
SELECT d.id,
       chunk.chunk_index,
       chunk.content,
       jsonb_build_object('source', 'opiagile-site', 'topic', chunk.topic)
FROM documento d
CROSS JOIN (
    VALUES
        (0, 'proposta', 'A Opiagile ajuda empresas a transformar documentos, processos, FAQs e manuais em respostas rápidas para equipes, clientes e parceiros. O objetivo é reduzir retrabalho, melhorar atendimento, acelerar decisões e tornar o conhecimento acessível em linguagem natural. A solução pode ser usada internamente por colaboradores ou externamente por clientes, leads e parceiros autorizados.'),
        (1, 'problemas', 'A Opiagile resolve problemas comuns de conhecimento espalhado em PDFs, planilhas, sistemas, conversas antigas e pessoas-chave. Esses problemas atrasam atendimento, aumentam retrabalho, deixam clientes esperando por respostas simples e dificultam decisões rápidas. A proposta é centralizar o acesso ao conhecimento sem obrigar a empresa a substituir seus sistemas atuais.'),
        (2, 'como-funciona', 'O funcionamento pode ser explicado em três passos simples: conectar materiais da empresa, permitir perguntas em linguagem natural e entregar respostas claras para agir com segurança. Os materiais podem incluir documentos, políticas, FAQs, manuais, processos, materiais comerciais, conteúdos de suporte e bases de conhecimento existentes.'),
        (3, 'beneficios', 'Os principais benefícios são respostas em segundos, menos retrabalho, atendimento mais consistente, onboarding mais rápido, decisões com mais confiança e conhecimento acessível para todo o time. A experiência pode apoiar atendimento ao cliente, times comerciais, operações internas, recursos humanos, jurídico, compliance, treinamento, onboarding, FAQ público, autoatendimento, manuais e suporte técnico.'),
        (4, 'integracoes', 'A Opiagile pode ser adaptada para chat web, WhatsApp, Microsoft Teams, Slack, Telegram, Instagram, Facebook, Zendesk, HubSpot, Salesforce, Google Calendar, Notion, CRM, suporte e portais web. As integrações dependem das regras e credenciais de cada plataforma, mas a arquitetura permite levar respostas para os canais onde as conversas já acontecem.'),
        (5, 'demo-rag', 'A demo RAG da Opiagile fica em demo-rag.opiagile.com e demonstra upload de documentos, perguntas em linguagem natural, respostas com fontes, workspaces separados, intenção, handoff humano e rastreabilidade. A demo serve para mostrar, de forma prática, como documentos e FAQs podem virar conversas com respostas claras.'),
        (6, 'chat-leads', 'O webchat da landing foi pensado para atender leads e interessados 24 horas por dia, 7 dias por semana. Ele deve explicar os serviços da Opiagile, orientar o visitante sobre a demo RAG, responder dúvidas comerciais e sugerir falar com uma pessoa quando o assunto exigir análise, proposta ou escopo específico. O visitante não deve enviar dados sensíveis pelo chat.'),
        (7, 'seguranca-lgpd', 'A Opiagile leva segurança da informação e LGPD a sério. Projetos devem considerar finalidade, minimização de dados, controle de acesso, organização por áreas, permissões, escopos de uso, logs responsáveis e governança antes de disponibilizar respostas para equipes ou clientes. Dados e materiais devem ser tratados de forma responsável e de acordo com a necessidade do negócio.'),
        (8, 'implantacao', 'A implantação recomendada é começar pequeno, com um caso de uso claro, provar valor e expandir para outras áreas. A Opiagile pode ajudar a organizar materiais, definir escopos, preparar fluxos de atendimento, configurar respostas, orientar integrações e criar encaminhamento humano quando necessário.'),
        (9, 'limites', 'A Opiagile não deve ser apresentada como substituta automática de todos os sistemas nem como atendimento humano completo. O uso correto é complementar a operação, organizar o acesso ao conhecimento e automatizar dúvidas recorrentes. Quando faltar informação segura, quando o usuário pedir uma pessoa ou quando o tema exigir análise especializada, o fluxo deve encaminhar para atendimento humano.')
) AS chunk(chunk_index, topic, content);
