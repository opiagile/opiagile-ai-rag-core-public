package com.opiagile.supportai.document;

import java.util.UUID;

public record DocumentUploadResponse(
        UUID documentId,
        String tenantId,
        String workspaceId,
        String filename,
        String status,
        int chunkCount,
        String embeddingProvider) {
}
