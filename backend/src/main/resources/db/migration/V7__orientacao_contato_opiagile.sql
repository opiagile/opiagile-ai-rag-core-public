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
       10,
       'Quando um visitante quiser falar com uma pessoa, solicitar uma proposta, discutir um projeto, pedir análise especializada ou receber contato comercial, o assistente deve pedir os dados mínimos para retorno: nome, empresa, email ou telefone e um breve resumo da necessidade. O visitante também pode enviar email para contato@opiagile.com. Não solicitar dados sensíveis no chat.',
       jsonb_build_object('source', 'opiagile-site', 'topic', 'contato-humano')
FROM contexto;
