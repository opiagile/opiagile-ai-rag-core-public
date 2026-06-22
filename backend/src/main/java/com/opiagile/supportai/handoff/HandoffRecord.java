package com.opiagile.supportai.handoff;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HandoffRecord(
        UUID id,
        UUID conversationId,
        String reason,
        String summary,
        String status,
        OffsetDateTime createdAt) {
}
