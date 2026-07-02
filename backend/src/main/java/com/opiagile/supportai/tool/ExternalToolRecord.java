package com.opiagile.supportai.tool;

import java.util.List;
import java.util.UUID;

public record ExternalToolRecord(
        UUID id,
        UUID tenantId,
        UUID workspaceId,
        String slug,
        String name,
        String type,
        String status,
        String description,
        List<String> allowedTables,
        int defaultLimit,
        int maxLimit) {
}
