package com.opiagile.supportai.provider;

import java.util.List;

public record ProviderStatusResponse(
        String status,
        ChatProviderStatusResponse chat,
        EmbeddingProviderStatusResponse embeddings,
        RetrievalProviderStatusResponse retrieval,
        ToolPlannerProviderStatusResponse toolPlanner,
        List<String> warnings) {
}
