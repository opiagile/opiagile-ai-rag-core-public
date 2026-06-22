package com.opiagile.supportai.chat;

import java.util.List;
import java.util.UUID;

public record ChatResponse(
        UUID conversationId,
        String answer,
        String intent,
        List<ChatSourceResponse> sources,
        boolean handoffRequired,
        String leadStatus,
        long latencyMs,
        String responseMode,
        String llmProvider,
        String model,
        String fallbackReason,
        Long llmLatencyMs,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens) {
}
