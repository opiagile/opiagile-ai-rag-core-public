package com.opiagile.supportai.rag;

import java.util.Optional;

public interface EmbeddingProvider {

    Optional<float[]> embed(String text);

    String providerName();
}
