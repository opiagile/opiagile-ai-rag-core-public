package com.opiagile.supportai.document;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentChunkResponse(
        UUID id,
        UUID documentId,
        int chunkIndex,
        String content,
        String metadata,
        OffsetDateTime createdAt) {
}
