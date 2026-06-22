package com.opiagile.supportai.rag;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "openai.embeddings.enabled", havingValue = "false", matchIfMissing = true)
public class NoopEmbeddingProvider implements EmbeddingProvider {

    @Override
    public Optional<float[]> embed(String text) {
        return Optional.empty();
    }

    @Override
    public String providerName() {
        return "noop";
    }
}
