package com.opiagile.supportai.rag;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

class SpringAiEmbeddingProviderTest {

    @Test
    void deveUsarEmbeddingModelDoSpringAi() {
        SpringAiEmbeddingProvider provider = new SpringAiEmbeddingProvider(new FixedEmbeddingModel(new float[] {0.1f, 0.2f}));

        assertThat(provider.embed("texto para embedding"))
                .hasValueSatisfying(values -> assertThat(values).containsExactly(0.1f, 0.2f));
        assertThat(provider.providerName()).isEqualTo("spring-ai");
    }

    @Test
    void deveIgnorarTextoVazio() {
        SpringAiEmbeddingProvider provider = new SpringAiEmbeddingProvider(new FixedEmbeddingModel(new float[] {0.1f}));

        assertThat(provider.embed(" ")).isEmpty();
    }

    @Test
    void deveVoltarParaTextualQuandoSpringAiFalhar() {
        SpringAiEmbeddingProvider provider = new SpringAiEmbeddingProvider(new FailingEmbeddingModel());

        assertThat(provider.embed("texto")).isEmpty();
    }

    private static class FixedEmbeddingModel implements EmbeddingModel {

        private final float[] embedding;

        FixedEmbeddingModel(float[] embedding) {
            this.embedding = embedding;
        }

        @Override
        public float[] embed(String text) {
            return embedding;
        }

        @Override
        public float[] embed(Document document) {
            return embedding;
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            throw new UnsupportedOperationException("Teste usa embed(String).");
        }
    }

    private static class FailingEmbeddingModel implements EmbeddingModel {

        @Override
        public float[] embed(String text) {
            throw new IllegalStateException("Falha token-nao-deve-vazar");
        }

        @Override
        public float[] embed(Document document) {
            throw new IllegalStateException("Falha");
        }

        @Override
        public EmbeddingResponse call(EmbeddingRequest request) {
            throw new UnsupportedOperationException("Teste usa embed(String).");
        }
    }
}
