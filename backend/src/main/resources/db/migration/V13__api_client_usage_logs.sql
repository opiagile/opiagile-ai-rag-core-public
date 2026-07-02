CREATE TABLE api_client_usage_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    api_client_id UUID REFERENCES api_clients(id) ON DELETE SET NULL,
    tenant_id UUID REFERENCES tenants(id) ON DELETE SET NULL,
    workspace_id UUID REFERENCES workspaces(id) ON DELETE SET NULL,
    method VARCHAR(12) NOT NULL,
    path TEXT NOT NULL,
    scope VARCHAR(80),
    status_code INTEGER NOT NULL,
    allowed BOOLEAN NOT NULL,
    blocked_reason VARCHAR(80),
    client_ip VARCHAR(80),
    user_agent VARCHAR(180),
    latency_ms INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_api_client_usage_logs_client_created ON api_client_usage_logs(api_client_id, created_at DESC);
CREATE INDEX idx_api_client_usage_logs_created ON api_client_usage_logs(created_at DESC);
CREATE INDEX idx_api_client_usage_logs_workspace_created ON api_client_usage_logs(tenant_id, workspace_id, created_at DESC);
