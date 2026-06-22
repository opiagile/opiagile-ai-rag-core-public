package com.opiagile.supportai.chat;

public record ChatGenerationResult(
        String answer,
        String responseMode,
        String llmProvider,
        String model,
        String fallbackReason,
        long llmLatencyMs,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens) {

    public static ChatGenerationResult demo(String answer, String fallbackReason, long latencyMs) {
        return new ChatGenerationResult(answer, "DEMO", "DEMO", "local-deterministico",
                fallbackReason, latencyMs, null, null, null);
    }
}
