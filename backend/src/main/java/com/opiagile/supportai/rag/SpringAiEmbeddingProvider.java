package com.opiagile.supportai.rag;

import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "openai.embeddings.enabled", havingValue = "true")
@ConditionalOnProperty(name = "openai.embeddings.provider", havingValue = "spring-ai")
public class SpringAiEmbeddingProvider implements EmbeddingProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringAiEmbeddingProvider.class);

    private final Supplier<EmbeddingModel> embeddingModelSupplier;

    @Autowired
    public SpringAiEmbeddingProvider(ObjectProvider<EmbeddingModel> embeddingModelProvider) {
        this.embeddingModelSupplier = embeddingModelProvider::getIfAvailable;
    }

    SpringAiEmbeddingProvider(EmbeddingModel embeddingModel) {
        this.embeddingModelSupplier = () -> embeddingModel;
    }

    @Override
    public Optional<float[]> embed(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        try {
            EmbeddingModel embeddingModel = embeddingModelSupplier.get();
            if (embeddingModel == null) {
                LOGGER.warn("EmbeddingModel do Spring AI não está disponível. Usando fallback textual.");
                return Optional.empty();
            }
            float[] embedding = embeddingModel.embed(text);
            if (embedding == null || embedding.length == 0) {
                return Optional.empty();
            }
            return Optional.of(embedding);
        } catch (RuntimeException exception) {
            LOGGER.warn("Falha ao gerar embedding via Spring AI. Usando fallback textual: {}", safeMessage(exception));
            return Optional.empty();
        }
    }

    @Override
    public String providerName() {
        return "spring-ai";
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.replaceAll("sk-[A-Za-z0-9_-]+", "sk-***");
    }
}
