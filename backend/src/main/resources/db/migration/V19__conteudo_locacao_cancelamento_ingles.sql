WITH contexto AS (
    SELECT d.id AS document_id
    FROM tenants t
    JOIN workspaces w ON w.tenant_id = t.id
    JOIN documents d ON d.tenant_id = t.id AND d.workspace_id = w.id
    WHERE t.slug = 'demo'
      AND w.slug = 'locacao-demo'
      AND d.filename = 'locacao-demo-faq.md'
    ORDER BY d.created_at
    LIMIT 1
)
INSERT INTO document_chunks (document_id, chunk_index, content, metadata)
SELECT document_id,
       8,
       'Cancellation, contract termination or ending a rental agreement must be routed to a human broker or analyst. The team validates contract term, penalty, inspection, key return, pending charges and the rules defined in the rental agreement.',
       jsonb_build_object('source', 'demo-rag-seed', 'workspace', 'locacao-demo', 'topic', 'rental-cancellation-english', 'language', 'en')
FROM contexto
ON CONFLICT DO NOTHING;
