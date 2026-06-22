package com.opiagile.supportai.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class EmbeddingVectorCodecTest {

    @Test
    void deveConverterEmbeddingParaFormatoPgvector() {
        String vector = EmbeddingVectorCodec.toPgVector(new float[] { 0.25f, -1.5f, 0.0f });

        assertThat(vector).isEqualTo("[0.25000000,-1.50000000,0.00000000]");
    }

    @Test
    void deveRejeitarEmbeddingVazioOuInvalido() {
        assertThatThrownBy(() -> EmbeddingVectorCodec.toPgVector(new float[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EmbeddingVectorCodec.toPgVector(new float[] { Float.NaN }))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
