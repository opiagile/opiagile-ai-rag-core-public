package com.opiagile.supportai.rag;

import java.util.UUID;

record StoredChunk(
        UUID chunkId,
        UUID documentId,
        String filename,
        String content,
        String language,
        Double score) {
}
