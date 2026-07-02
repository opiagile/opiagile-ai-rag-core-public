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
       jsonb_build_object('source', 'opiagile-site', 'topic', chunk.topic, 'language', chunk.language, 'updated_by', 'V10')
FROM contexto
CROSS JOIN (
    VALUES
        (19, 'integracao-banco-dados', 'pt', 'A Opiagile pode ser adaptada para consultar bases de dados e sistemas internos quando o caso de uso exigir. Esse tipo de integração deve ser feito com escopo claro, credenciais seguras, permissões mínimas, logs responsáveis, LGPD, limites de consulta e regras para não expor dados sensíveis. Para uma validação inicial, a recomendação é começar com uma base de exemplo, uma tabela ou uma visão controlada, antes de conectar dados críticos.'),
        (20, 'english-database-integration', 'en', 'Opiagile can be adapted to query databases and internal systems when the use case requires it. This integration should use clear scope, secure credentials, minimum permissions, responsible logs, privacy controls, query limits and rules to avoid exposing sensitive data. For an initial validation, start with sample data, one table or a controlled view before connecting critical data.'),
        (21, 'spanish-database-integration', 'es', 'Opiagile puede adaptarse para consultar bases de datos y sistemas internos cuando el caso de uso lo requiere. Esta integración debe tener alcance claro, credenciales seguras, permisos mínimos, registros responsables, controles de privacidad, límites de consulta y reglas para no exponer datos sensibles. Para una validación inicial, se recomienda empezar con datos de ejemplo, una tabla o una vista controlada antes de conectar datos críticos.')
) AS chunk(chunk_index, topic, language, content)
ON CONFLICT DO NOTHING;
