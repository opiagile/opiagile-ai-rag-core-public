CREATE TABLE api_clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    workspace_id UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    key_prefix VARCHAR(32) NOT NULL,
    key_hash CHAR(64) NOT NULL UNIQUE,
    scopes TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    rate_limit_per_minute INTEGER NOT NULL DEFAULT 60,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at TIMESTAMPTZ,
    CONSTRAINT ck_api_clients_status CHECK (status IN ('ACTIVE', 'REVOKED')),
    CONSTRAINT ck_api_clients_rate_limit CHECK (rate_limit_per_minute >= 1)
);

CREATE INDEX idx_api_clients_tenant_workspace ON api_clients(tenant_id, workspace_id);
CREATE INDEX idx_api_clients_status ON api_clients(status);
