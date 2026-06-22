ALTER TABLE retrieval_logs
    ADD COLUMN intent VARCHAR(60),
    ADD COLUMN handoff_required BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN fallback_reason TEXT,
    ADD COLUMN provider VARCHAR(80) NOT NULL DEFAULT 'local-text';

CREATE INDEX idx_retrieval_logs_intent_created ON retrieval_logs(intent, created_at);
