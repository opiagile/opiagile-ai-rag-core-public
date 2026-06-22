package com.opiagile.supportai.observability;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RetrievalTraceResponse(
        UUID id,
        UUID conversationId,
        String query,
        String retrievedChunks,
        int latencyMs,
        String intent,
        boolean handoffRequired,
        String fallbackReason,
        String provider,
        String responseMode,
        String llmProvider,
        String model,
        Integer llmLatencyMs,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        OffsetDateTime createdAt) {
}
