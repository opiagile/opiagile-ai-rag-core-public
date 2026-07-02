ALTER TABLE developer_access_requests
    ADD COLUMN notification_email_sent BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN notification_email_sent_at TIMESTAMPTZ,
    ADD COLUMN notification_email_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN notification_email_last_attempt_at TIMESTAMPTZ,
    ADD COLUMN notification_email_next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN notification_email_last_error VARCHAR(240);

ALTER TABLE developer_access_requests
    ALTER COLUMN notification_email_sent SET DEFAULT false;

CREATE INDEX idx_developer_access_requests_email_queue
    ON developer_access_requests(notification_email_sent, notification_email_next_attempt_at, created_at)
    WHERE notification_email_sent = false;
