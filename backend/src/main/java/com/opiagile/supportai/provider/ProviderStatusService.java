package com.opiagile.supportai.provider;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.opiagile.supportai.chat.OpenAiChatModelProvider;
import com.opiagile.supportai.rag.EmbeddingProvider;

@Service
public class ProviderStatusService {

    private final OpenAiChatModelProvider openAiChatModelProvider;
    private final EmbeddingProvider embeddingProvider;
    private final String responseMode;
    private final String llmProvider;
    private final boolean embeddingsEnabled;
    private final String embeddingModel;
    private final int embeddingDimensions;
    private final boolean toolsPlannerLlmEnabled;

    public ProviderStatusService(
            OpenAiChatModelProvider openAiChatModelProvider,
            EmbeddingProvider embeddingProvider,
            @Value("${chat.response-mode:DEMO}") String responseMode,
            @Value("${llm.provider:DEMO}") String llmProvider,
            @Value("${openai.embeddings.enabled:false}") boolean embeddingsEnabled,
            @Value("${openai.embedding-model:text-embedding-3-small}") String embeddingModel,
            @Value("${openai.embedding-dimensions:1536}") int embeddingDimensions,
            @Value("${tools.planner.llm.enabled:true}") boolean toolsPlannerLlmEnabled) {
        this.openAiChatModelProvider = openAiChatModelProvider;
        this.embeddingProvider = embeddingProvider;
        this.responseMode = normalize(responseMode);
        this.llmProvider = normalize(llmProvider);
        this.embeddingsEnabled = embeddingsEnabled;
        this.embeddingModel = blankFallback(embeddingModel, "text-embedding-3-small");
        this.embeddingDimensions = Math.max(1, embeddingDimensions);
        this.toolsPlannerLlmEnabled = toolsPlannerLlmEnabled;
    }

    public ProviderStatusResponse status() {
        boolean openAiConfigured = openAiChatModelProvider.isConfigured();
        boolean openAiChatRequested = openAiChatRequested();
        List<String> warnings = warnings(openAiConfigured, openAiChatRequested);
        return new ProviderStatusResponse(
                warnings.isEmpty() ? "OK" : "ATENCAO",
                chatStatus(openAiConfigured, openAiChatRequested),
                embeddingStatus(openAiConfigured),
                retrievalStatus(openAiConfigured),
                toolPlannerStatus(openAiConfigured),
                warnings);
    }

    private ChatProviderStatusResponse chatStatus(boolean openAiConfigured, boolean openAiChatRequested) {
        String activeProvider = openAiChatRequested && openAiConfigured ? "OPENAI" : "DEMO";
        String model = "OPENAI".equals(activeProvider) ? openAiChatModelProvider.model() : "local-deterministico";
        String status = openAiChatRequested && !openAiConfigured
                ? "FALLBACK_DEMO_POR_CHAVE_AUSENTE"
                : "OPERACIONAL";
        return new ChatProviderStatusResponse(
                responseMode,
                llmProvider,
                activeProvider,
                model,
                openAiConfigured,
                true,
                "DEMO",
                status);
    }

    private EmbeddingProviderStatusResponse embeddingStatus(boolean openAiConfigured) {
        String status;
        if (!embeddingsEnabled) {
            status = "DESABILITADO_FALLBACK_TEXTUAL";
        } else if (!openAiConfigured) {
            status = "FALLBACK_TEXTUAL_POR_CHAVE_AUSENTE";
        } else {
            status = "OPERACIONAL";
        }
        return new EmbeddingProviderStatusResponse(
                embeddingsEnabled,
                embeddingProvider.providerName().toUpperCase(),
                embeddingModel,
                embeddingDimensions,
                openAiConfigured,
                true,
                "LOCAL_TEXT",
                status);
    }

    private RetrievalProviderStatusResponse retrievalStatus(boolean openAiConfigured) {
        boolean vectorReadyByConfig = embeddingsEnabled && openAiConfigured;
        String activeStrategy = vectorReadyByConfig ? "PGVECTOR_COM_FALLBACK_TEXTUAL" : "LOCAL_TEXT";
        String status = vectorReadyByConfig
                ? "PGVECTOR_QUANDO_CHUNKS_TIVEREM_VETORES"
                : "FALLBACK_TEXTUAL";
        return new RetrievalProviderStatusResponse(
                activeStrategy,
                vectorReadyByConfig,
                true,
                "LOCAL_TEXT",
                status);
    }

    private ToolPlannerProviderStatusResponse toolPlannerStatus(boolean openAiConfigured) {
        String activeProvider = toolsPlannerLlmEnabled && openAiConfigured ? "OPENAI" : "NONE";
        String status;
        if (!toolsPlannerLlmEnabled) {
            status = "DESABILITADO";
        } else if (!openAiConfigured) {
            status = "INATIVO_POR_CHAVE_AUSENTE";
        } else {
            status = "OPERACIONAL";
        }
        return new ToolPlannerProviderStatusResponse(
                toolsPlannerLlmEnabled,
                activeProvider,
                openAiConfigured,
                status);
    }

    private List<String> warnings(boolean openAiConfigured, boolean openAiChatRequested) {
        List<String> result = new ArrayList<>();
        if (openAiChatRequested && !openAiConfigured) {
            result.add("OPENAI_CHAT_SOLICITADO_SEM_CHAVE; respostas usam fallback DEMO.");
        }
        if (embeddingsEnabled && !openAiConfigured) {
            result.add("OPENAI_EMBEDDINGS_HABILITADO_SEM_CHAVE; recuperação usa fallback textual.");
        }
        if (!embeddingsEnabled) {
            result.add("OPENAI_EMBEDDINGS_DESABILITADO; recuperação usa texto local até habilitar embeddings.");
        }
        if (toolsPlannerLlmEnabled && !openAiConfigured) {
            result.add("TOOLS_PLANNER_LLM_SEM_CHAVE; planner LLM fica inativo.");
        }
        return result;
    }

    private boolean openAiChatRequested() {
        return "LLM".equals(responseMode) || "OPENAI".equals(llmProvider);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "DEMO";
        }
        return value.trim().toUpperCase();
    }

    private String blankFallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
