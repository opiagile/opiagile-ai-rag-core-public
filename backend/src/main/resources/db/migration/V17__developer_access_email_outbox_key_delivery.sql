CREATE TABLE developer_access_email_outbox (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID REFERENCES developer_access_requests(id) ON DELETE CASCADE,
    email_type VARCHAR(60) NOT NULL,
    recipient VARCHAR(180) NOT NULL,
    subject VARCHAR(220) NOT NULL,
    html_body TEXT NOT NULL,
    text_body TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_attempt_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    last_error VARCHAR(240),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_developer_access_email_outbox_status
        CHECK (status IN ('PENDING', 'SENT', 'FAILED'))
);

CREATE INDEX idx_developer_access_email_outbox_pending
    ON developer_access_email_outbox(status, next_attempt_at, created_at)
    WHERE status = 'PENDING';

CREATE INDEX idx_developer_access_email_outbox_request
    ON developer_access_email_outbox(request_id, email_type);

CREATE TABLE developer_access_key_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID NOT NULL REFERENCES developer_access_requests(id) ON DELETE CASCADE,
    api_client_id UUID NOT NULL REFERENCES api_clients(id) ON DELETE CASCADE,
    token_hash CHAR(64) NOT NULL UNIQUE,
    encrypted_api_key TEXT NOT NULL,
    key_prefix VARCHAR(32) NOT NULL,
    tenant_slug VARCHAR(80) NOT NULL,
    workspace_slug VARCHAR(80) NOT NULL,
    scopes TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
    rate_limit_per_minute INTEGER NOT NULL,
    sandbox_expires_at TIMESTAMPTZ,
    delivery_expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    retention_notice TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_developer_access_key_deliveries_token
    ON developer_access_key_deliveries(token_hash)
    WHERE consumed_at IS NULL;

CREATE INDEX idx_developer_access_key_deliveries_request
    ON developer_access_key_deliveries(request_id);
