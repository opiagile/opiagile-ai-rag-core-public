package com.opiagile.supportai.provider;

public record EmbeddingProviderStatusResponse(
        boolean enabled,
        String activeProvider,
        String model,
        int dimensions,
        boolean openAiApiKeyConfigured,
        boolean fallbackEnabled,
        String fallbackProvider,
        String status) {
}
