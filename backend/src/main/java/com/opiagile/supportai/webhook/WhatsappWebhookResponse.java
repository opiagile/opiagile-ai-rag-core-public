package com.opiagile.supportai.webhook;

import java.util.UUID;

public record WhatsappWebhookResponse(
        String provider,
        String to,
        UUID conversationId,
        String message,
        String intent,
        boolean handoffRequired,
        String leadStatus) {
}
