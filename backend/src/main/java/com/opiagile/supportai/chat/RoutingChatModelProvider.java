package com.opiagile.supportai.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RoutingChatModelProvider implements ChatModelProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoutingChatModelProvider.class);

    private final DemoChatModelProvider demoProvider;
    private final OpenAiChatModelProvider openAiProvider;
    private final String responseMode;
    private final String llmProvider;

    public RoutingChatModelProvider(
            DemoChatModelProvider demoProvider,
            OpenAiChatModelProvider openAiProvider,
            @Value("${chat.response-mode:DEMO}") String responseMode,
            @Value("${llm.provider:DEMO}") String llmProvider) {
        this.demoProvider = demoProvider;
        this.openAiProvider = openAiProvider;
        this.responseMode = normalize(responseMode);
        this.llmProvider = normalize(llmProvider);
    }

    @Override
    public ChatGenerationResult generate(ChatPrompt prompt) {
        if (isOpenAiRequested() && !openAiProvider.isConfigured()) {
            ChatGenerationResult fallback = demoProvider.generate(prompt);
            return withFallbackReason(fallback, "OPENAI_API_KEY_AUSENTE");
        }
        if (isOpenAiRequested()) {
            try {
                return openAiProvider.generate(prompt);
            } catch (RuntimeException exception) {
                LOGGER.warn("Falha ao gerar resposta com OpenAI. Usando fallback local: {}", exception.getMessage());
                return withFallbackReason(demoProvider.generate(prompt), "OPENAI_FALLBACK: " + safeMessage(exception));
            }
        }
        return demoProvider.generate(prompt);
    }

    @Override
    public String providerName() {
        if (isOpenAiRequested() && openAiProvider.isConfigured()) {
            return "OPENAI";
        }
        return "DEMO";
    }

    private boolean isOpenAiRequested() {
        return "LLM".equals(responseMode) || "OPENAI".equals(llmProvider);
    }

    private ChatGenerationResult withFallbackReason(ChatGenerationResult fallback, String fallbackReason) {
        return new ChatGenerationResult(
                fallback.answer(),
                fallback.responseMode(),
                fallback.llmProvider(),
                fallback.model(),
                fallbackReason,
                fallback.llmLatencyMs(),
                fallback.promptTokens(),
                fallback.completionTokens(),
                fallback.totalTokens());
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "DEMO";
        }
        return value.trim().toUpperCase();
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.replaceAll("sk-[A-Za-z0-9_-]+", "sk-***");
    }
}
