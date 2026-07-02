ALTER TABLE tenants
    ADD COLUMN sandbox BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN expires_at TIMESTAMPTZ;

ALTER TABLE workspaces
    ADD COLUMN sandbox BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN expires_at TIMESTAMPTZ;

ALTER TABLE api_clients
    ADD COLUMN expires_at TIMESTAMPTZ;

ALTER TABLE developer_access_requests
    ADD COLUMN approved_api_client_id UUID REFERENCES api_clients(id) ON DELETE SET NULL,
    ADD COLUMN approved_tenant_slug VARCHAR(80),
    ADD COLUMN approved_workspace_slug VARCHAR(80),
    ADD COLUMN sandbox_expires_at TIMESTAMPTZ,
    ADD COLUMN sandbox_deleted_at TIMESTAMPTZ;

CREATE INDEX idx_tenants_sandbox_expires
    ON tenants(sandbox, expires_at)
    WHERE sandbox = true;

CREATE INDEX idx_workspaces_sandbox_expires
    ON workspaces(sandbox, expires_at)
    WHERE sandbox = true;

CREATE INDEX idx_api_clients_expires
    ON api_clients(expires_at)
    WHERE expires_at IS NOT NULL;

CREATE INDEX idx_developer_access_requests_sandbox_cleanup
    ON developer_access_requests(sandbox_expires_at, sandbox_deleted_at)
    WHERE sandbox_expires_at IS NOT NULL;
