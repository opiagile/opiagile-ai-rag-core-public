package com.opiagile.supportai.handoff;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HandoffResponse(
        UUID id,
        UUID conversationId,
        String reason,
        String summary,
        String status,
        OffsetDateTime createdAt) {

    public static HandoffResponse from(HandoffRecord handoff) {
        return new HandoffResponse(
                handoff.id(),
                handoff.conversationId(),
                handoff.reason(),
                handoff.summary(),
                handoff.status(),
                handoff.createdAt());
    }
}
