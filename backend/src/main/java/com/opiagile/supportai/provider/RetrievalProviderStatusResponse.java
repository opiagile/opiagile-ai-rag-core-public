package com.opiagile.supportai.provider;

public record RetrievalProviderStatusResponse(
        String activeStrategy,
        boolean pgvectorReadyByConfiguration,
        boolean fallbackEnabled,
        String fallbackProvider,
        String status) {
}
