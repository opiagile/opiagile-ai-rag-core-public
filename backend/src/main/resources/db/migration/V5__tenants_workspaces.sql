CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE workspaces (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    slug VARCHAR(80) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_workspaces_tenant_slug UNIQUE (tenant_id, slug)
);

INSERT INTO tenants (slug, name)
VALUES ('demo', 'Demo Opiagile')
ON CONFLICT (slug) DO NOTHING;

INSERT INTO workspaces (tenant_id, slug, name, description)
SELECT t.id, workspace.slug, workspace.name, workspace.description
FROM tenants t
CROSS JOIN (
    VALUES
        ('clinica-demo', 'Clínica Demo', 'Base de exemplo para atendimento de clínica ou consultório.'),
        ('atendimento-demo', 'Atendimento Demo', 'Base de exemplo para FAQ, suporte e handoff operacional.'),
        ('locacao-demo', 'Locação Demo', 'Base de exemplo para locação imobiliária e dúvidas comerciais.')
) AS workspace(slug, name, description)
WHERE t.slug = 'demo'
ON CONFLICT (tenant_id, slug) DO NOTHING;

ALTER TABLE documents
    ADD COLUMN tenant_id UUID,
    ADD COLUMN workspace_id UUID;

ALTER TABLE conversations
    ADD COLUMN tenant_id UUID,
    ADD COLUMN workspace_id UUID;

ALTER TABLE retrieval_logs
    ADD COLUMN tenant_id UUID,
    ADD COLUMN workspace_id UUID;

UPDATE documents
SET tenant_id = t.id,
    workspace_id = w.id
FROM tenants t
JOIN workspaces w ON w.tenant_id = t.id AND w.slug = 'clinica-demo'
WHERE t.slug = 'demo'
  AND documents.tenant_id IS NULL;

UPDATE conversations
SET tenant_id = t.id,
    workspace_id = w.id
FROM tenants t
JOIN workspaces w ON w.tenant_id = t.id AND w.slug = 'clinica-demo'
WHERE t.slug = 'demo'
  AND conversations.tenant_id IS NULL;

UPDATE retrieval_logs
SET tenant_id = t.id,
    workspace_id = w.id
FROM tenants t
JOIN workspaces w ON w.tenant_id = t.id AND w.slug = 'clinica-demo'
WHERE t.slug = 'demo'
  AND retrieval_logs.tenant_id IS NULL;

ALTER TABLE documents
    ALTER COLUMN tenant_id SET NOT NULL,
    ALTER COLUMN workspace_id SET NOT NULL,
    ADD CONSTRAINT fk_documents_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    ADD CONSTRAINT fk_documents_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id);

ALTER TABLE conversations
    ALTER COLUMN tenant_id SET NOT NULL,
    ALTER COLUMN workspace_id SET NOT NULL,
    ADD CONSTRAINT fk_conversations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    ADD CONSTRAINT fk_conversations_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id);

ALTER TABLE retrieval_logs
    ADD CONSTRAINT fk_retrieval_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    ADD CONSTRAINT fk_retrieval_logs_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id);

CREATE INDEX idx_documents_workspace_created ON documents(tenant_id, workspace_id, created_at DESC);
CREATE INDEX idx_conversations_workspace_external ON conversations(tenant_id, workspace_id, external_channel, external_contact_id);
CREATE INDEX idx_retrieval_logs_workspace_created ON retrieval_logs(tenant_id, workspace_id, created_at DESC);
