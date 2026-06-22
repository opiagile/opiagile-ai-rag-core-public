CREATE TABLE whatsapp_webhook_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider VARCHAR(80) NOT NULL,
    inbound_message_id VARCHAR(160),
    conversation_id UUID REFERENCES conversations(id) ON DELETE SET NULL,
    masked_phone VARCHAR(40),
    event_type VARCHAR(80) NOT NULL,
    processed BOOLEAN NOT NULL DEFAULT false,
    blocked_reason TEXT,
    dry_run BOOLEAN NOT NULL DEFAULT true,
    send_enabled BOOLEAN NOT NULL DEFAULT false,
    outbound_status VARCHAR(80),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_whatsapp_events_message_id ON whatsapp_webhook_events(inbound_message_id);
CREATE INDEX idx_whatsapp_events_created ON whatsapp_webhook_events(created_at);
CREATE INDEX idx_whatsapp_events_conversation ON whatsapp_webhook_events(conversation_id);
