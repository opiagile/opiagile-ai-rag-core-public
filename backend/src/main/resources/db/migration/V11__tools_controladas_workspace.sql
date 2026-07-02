CREATE TABLE external_tools (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    slug VARCHAR(100) NOT NULL,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(60) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    description TEXT,
    config JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_external_tools_workspace_slug UNIQUE (tenant_id, workspace_id, slug),
    CONSTRAINT ck_external_tools_type CHECK (type IN ('SQL_READ_ONLY')),
    CONSTRAINT ck_external_tools_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE external_tool_execution_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    tool_id UUID NOT NULL REFERENCES external_tools(id) ON DELETE CASCADE,
    tool_slug VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL,
    query_preview TEXT,
    row_count INTEGER NOT NULL DEFAULT 0,
    latency_ms INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_external_tools_workspace_status
    ON external_tools(tenant_id, workspace_id, status, slug);

CREATE INDEX idx_external_tool_execution_logs_workspace_created
    ON external_tool_execution_logs(tenant_id, workspace_id, created_at DESC);

INSERT INTO external_tools (tenant_id, workspace_id, slug, name, type, status, description, config)
SELECT t.id,
       w.id,
       'base-conhecimento-readonly',
       'Base de conhecimento somente leitura',
       'SQL_READ_ONLY',
       'ACTIVE',
       'Permite consultar metadados e trechos da base de conhecimento do workspace em modo somente leitura, com tabelas permitidas e limite de linhas.',
       jsonb_build_object(
           'allowedTables', jsonb_build_array('documents', 'document_chunks', 'retrieval_logs'),
           'defaultLimit', 20,
           'maxLimit', 50
       )
FROM tenants t
JOIN workspaces w ON w.tenant_id = t.id
WHERE t.slug = 'opiagile'
  AND w.slug = 'opiagile-rag'
ON CONFLICT (tenant_id, workspace_id, slug) DO NOTHING;
