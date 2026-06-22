ALTER TABLE retrieval_logs
    ADD COLUMN response_mode VARCHAR(40) NOT NULL DEFAULT 'DEMO',
    ADD COLUMN llm_provider VARCHAR(80) NOT NULL DEFAULT 'DEMO',
    ADD COLUMN model VARCHAR(120),
    ADD COLUMN llm_latency_ms INTEGER,
    ADD COLUMN prompt_tokens INTEGER,
    ADD COLUMN completion_tokens INTEGER,
    ADD COLUMN total_tokens INTEGER;

CREATE INDEX idx_retrieval_logs_response_mode_created ON retrieval_logs(response_mode, created_at);
