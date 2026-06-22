package com.opiagile.supportai.rag;

import java.util.Locale;

public final class EmbeddingVectorCodec {

    private EmbeddingVectorCodec() {
    }

    public static String toPgVector(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new IllegalArgumentException("Embedding vazio não pode ser convertido para pgvector.");
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            float value = embedding[i];
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException("Embedding contém valor não finito.");
            }
            if (i > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.ROOT, "%.8f", value));
        }
        return builder.append(']').toString();
    }
}
