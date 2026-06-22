package com.opiagile.supportai.webhook;

import java.util.UUID;

public record WhatsAppMetaWebhookResponse(
        String provider,
        String status,
        boolean processed,
        boolean dryRun,
        boolean sendEnabled,
        String blockedReason,
        String maskedPhone,
        UUID conversationId,
        String outboundStatus) {
}
