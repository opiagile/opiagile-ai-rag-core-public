package com.opiagile.supportai.rag;

import java.util.UUID;

public record RetrievedChunk(
        UUID chunkId,
        UUID documentId,
        String filename,
        String content,
        double score,
        String excerpt,
        String retrievalProvider) {
}
