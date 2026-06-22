package com.opiagile.supportai.conversation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        String role,
        String content,
        String intent,
        OffsetDateTime createdAt) {

    public static MessageResponse from(MessageRecord message) {
        return new MessageResponse(
                message.id(),
                message.conversationId(),
                message.role(),
                message.content(),
                message.intent(),
                message.createdAt());
    }
}
