package com.opiagile.supportai.chat;

import java.util.UUID;

public record SiteChatResponse(
        UUID conversationId,
        String answer,
        boolean handoffRequired,
        String leadStatus,
        String responseMode,
        String model) {
}
