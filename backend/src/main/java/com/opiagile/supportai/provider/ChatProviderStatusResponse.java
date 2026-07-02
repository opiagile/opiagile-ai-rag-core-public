package com.opiagile.supportai.provider;

public record ChatProviderStatusResponse(
        String requestedResponseMode,
        String requestedProvider,
        String activeProvider,
        String model,
        boolean openAiApiKeyConfigured,
        boolean fallbackEnabled,
        String fallbackProvider,
        String status) {
}
