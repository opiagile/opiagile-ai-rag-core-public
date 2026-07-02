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
UPDATE document_chunks dc
SET content = 'Quando um visitante quiser testar, validar, ver uma demo ou experimentar a solução, o assistente deve orientar primeiro para https://demo-rag.opiagile.com. A demo RAG permite subir documentos, fazer perguntas em linguagem natural, ver respostas com fontes, usar workspaces separados, testar handoff e entender como documentos e FAQs podem virar conversas. Não pedir dados de contato quando o visitante apenas quer testar ou validar a demo.',
    metadata = jsonb_build_object('source', 'opiagile-site', 'topic', 'demo-online', 'updated_by', 'V9')
FROM contexto
WHERE dc.document_id = contexto.document_id
  AND dc.chunk_index = 5;

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
UPDATE document_chunks dc
SET content = 'Quando um visitante quiser falar com uma pessoa, solicitar proposta, orçamento, reunião, análise especializada ou contato comercial, o assistente deve pedir dados mínimos para retorno: nome, empresa, email ou telefone e um breve resumo da necessidade. Em dúvidas informativas, demonstrações ou pedidos para testar a solução, o assistente deve responder a dúvida primeiro e oferecer contato apenas como opção secundária, sem insistir. O visitante não deve enviar dados sensíveis no chat.',
    metadata = jsonb_build_object('source', 'opiagile-site', 'topic', 'contato-humano', 'updated_by', 'V9')
FROM contexto
WHERE dc.document_id = contexto.document_id
  AND dc.chunk_index = 10;

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
       jsonb_build_object('source', 'opiagile-site', 'topic', chunk.topic, 'language', chunk.language, 'updated_by', 'V9')
FROM contexto
CROSS JOIN (
    VALUES
        (17, 'english-demo-online', 'en', 'If a visitor wants to test, validate, try or see the solution working, the assistant should first point to https://demo-rag.opiagile.com. The RAG demo lets visitors upload a document, ask questions in natural language, see answers with sources and understand how separated workspaces support different use cases. Do not ask for contact details when the visitor only wants to test the demo.'),
        (18, 'spanish-demo-online', 'es', 'Si un visitante quiere probar, validar, experimentar o ver la solución funcionando, el asistente debe indicar primero https://demo-rag.opiagile.com. La demo RAG permite subir un documento, hacer preguntas en lenguaje natural, ver respuestas con fuentes y entender cómo los espacios separados apoyan diferentes casos de uso. No pedir datos de contacto cuando el visitante solo quiere probar la demo.')
) AS chunk(chunk_index, topic, language, content)
ON CONFLICT DO NOTHING;
