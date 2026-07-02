CREATE TABLE developer_access_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(160) NOT NULL,
    company VARCHAR(180),
    email VARCHAR(180) NOT NULL,
    use_case TEXT NOT NULL,
    requested_resources TEXT,
    source VARCHAR(80) NOT NULL DEFAULT 'developers-portal',
    status VARCHAR(40) NOT NULL DEFAULT 'NEW',
    client_ip VARCHAR(80),
    user_agent VARCHAR(220),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_developer_access_requests_status CHECK (status IN ('NEW', 'CONTACTED', 'APPROVED', 'REJECTED', 'ARCHIVED'))
);

CREATE INDEX idx_developer_access_requests_status_created ON developer_access_requests(status, created_at DESC);
CREATE INDEX idx_developer_access_requests_email ON developer_access_requests(email);
