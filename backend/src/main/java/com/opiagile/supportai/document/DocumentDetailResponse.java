package com.opiagile.supportai.document;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentDetailResponse(
        UUID id,
        String tenantId,
        String workspaceId,
        String filename,
        String contentType,
        String sourceType,
        String status,
        int chunkCount,
        OffsetDateTime createdAt) {
}
