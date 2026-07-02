package com.opiagile.supportai.provider;

public record ToolPlannerProviderStatusResponse(
        boolean enabled,
        String activeProvider,
        boolean openAiApiKeyConfigured,
        String status) {
}
