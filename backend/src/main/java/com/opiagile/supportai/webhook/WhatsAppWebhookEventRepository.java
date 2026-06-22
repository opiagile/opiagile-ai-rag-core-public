package com.opiagile.supportai.webhook;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class WhatsAppWebhookEventRepository {

    private final JdbcClient jdbc;

    public WhatsAppWebhookEventRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void save(
            String provider,
            String inboundMessageId,
            UUID conversationId,
            String maskedPhone,
            String eventType,
            boolean processed,
            String blockedReason,
            boolean dryRun,
            boolean sendEnabled,
            String outboundStatus) {
        jdbc.sql("""
                INSERT INTO whatsapp_webhook_events (
                    provider, inbound_message_id, conversation_id, masked_phone, event_type, processed,
                    blocked_reason, dry_run, send_enabled, outbound_status
                ) VALUES (
                    :provider, :inboundMessageId, :conversationId, :maskedPhone, :eventType, :processed,
                    :blockedReason, :dryRun, :sendEnabled, :outboundStatus
                )
                """)
                .param("provider", provider)
                .param("inboundMessageId", inboundMessageId)
                .param("conversationId", conversationId)
                .param("maskedPhone", maskedPhone)
                .param("eventType", eventType)
                .param("processed", processed)
                .param("blockedReason", blockedReason)
                .param("dryRun", dryRun)
                .param("sendEnabled", sendEnabled)
                .param("outboundStatus", outboundStatus)
                .update();
    }
}
