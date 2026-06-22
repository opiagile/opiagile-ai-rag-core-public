package com.opiagile.supportai.conversation;

import java.util.UUID;

public record ConversationSummaryResponse(
        UUID conversationId,
        String summary) {
}
