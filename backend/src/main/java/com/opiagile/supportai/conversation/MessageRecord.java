package com.opiagile.supportai.conversation;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MessageRecord(
        UUID id,
        UUID conversationId,
        String role,
        String content,
        String intent,
        OffsetDateTime createdAt) {
}
