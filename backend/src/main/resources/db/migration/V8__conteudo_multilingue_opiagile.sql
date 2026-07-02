WITH contexto AS (
    SELECT d.id AS document_id
    FROM tenants t
    JOIN workspaces w ON w.tenant_id = t.id
    JOIN documents d ON d.tenant_id = t.id AND d.workspace_id = w.id
    WHERE t.slug = 'opiagile'
      AND w.slug = 'opiagile-rag'
      AND d.filename = 'opiagile-rag-conhecimento.md'
    ORDER BY d.created_at
    LIMIT 1
)
INSERT INTO document_chunks (document_id, chunk_index, content, metadata)
SELECT document_id,
       chunk.chunk_index,
       chunk.content,
       jsonb_build_object('source', 'opiagile-site', 'topic', chunk.topic, 'language', chunk.language)
FROM contexto
CROSS JOIN (
    VALUES
        (11, 'english-services', 'en', 'Opiagile helps companies transform documents, FAQs, manuals, processes and internal knowledge into fast, reliable answers for teams, customers and partners. The goal is to reduce rework, improve service consistency, accelerate decisions and make knowledge easier to access through natural language.'),
        (12, 'english-integrations', 'en', 'Opiagile can be adapted to web chat, WhatsApp, Microsoft Teams, Slack, Telegram, Instagram, Facebook, Zendesk, HubSpot, Salesforce, Google Calendar, Notion, CRM, support portals and existing knowledge bases. Integrations depend on each platform rules and credentials, but the architecture is designed to bring answers to the channels where conversations already happen.'),
        (13, 'english-security-contact', 'en', 'Opiagile takes information security and LGPD seriously. Projects should consider purpose, data minimization, access control, areas and permissions, usage scope, responsible logs and governance. If a visitor wants a proposal, a specialist or human contact, the assistant should ask for name, company, email or phone and a short summary of the need.'),
        (14, 'spanish-services', 'es', 'Opiagile ayuda a las empresas a transformar documentos, preguntas frecuentes, manuales, procesos y conocimiento interno en respuestas rápidas y confiables para equipos, clientes y socios. El objetivo es reducir retrabajo, mejorar la consistencia del servicio, acelerar decisiones y facilitar el acceso al conocimiento en lenguaje natural.'),
        (15, 'spanish-integrations', 'es', 'Opiagile puede adaptarse a chat web, WhatsApp, Microsoft Teams, Slack, Telegram, Instagram, Facebook, Zendesk, HubSpot, Salesforce, Google Calendar, Notion, CRM, portales de soporte y bases de conocimiento existentes. Las integraciones dependen de las reglas y credenciales de cada plataforma.'),
        (16, 'spanish-security-contact', 'es', 'Opiagile toma en serio la seguridad de la información y la LGPD. Los proyectos deben considerar finalidad, minimización de datos, control de acceso, áreas y permisos, alcance de uso, registros responsables y gobernanza. Si un visitante quiere una propuesta, un especialista o contacto humano, el asistente debe pedir nombre, empresa, email o teléfono y un breve resumen de la necesidad.')
) AS chunk(chunk_index, topic, language, content);
