package com.opiagile.supportai.webhook;

public record WhatsAppInboundMessage(
        String eventType,
        String phoneNumberId,
        String from,
        String contactName,
        String messageId,
        String timestamp,
        String messageType,
        String text) {

    public boolean isTextMessage() {
        return "message_text".equals(eventType) && text != null && !text.isBlank();
    }
}
