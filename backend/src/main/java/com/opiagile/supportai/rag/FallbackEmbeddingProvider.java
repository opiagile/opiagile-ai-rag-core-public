package com.opiagile.supportai.rag;

import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(EmbeddingProvider.class)
public class FallbackEmbeddingProvider implements EmbeddingProvider {

    @Override
    public Optional<float[]> embed(String text) {
        return Optional.empty();
    }

    @Override
    public String providerName() {
        return "fallback-text";
    }
}
