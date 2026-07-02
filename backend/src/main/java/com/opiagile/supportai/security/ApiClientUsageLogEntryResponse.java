package com.opiagile.supportai.security;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApiClientUsageLogEntryResponse(
        UUID id,
        UUID apiClientId,
        String apiClientName,
        String tenantSlug,
        String workspaceSlug,
        String method,
        String path,
        String scope,
        int statusCode,
        boolean allowed,
        String blockedReason,
        String clientIp,
        String userAgent,
        int latencyMs,
        OffsetDateTime createdAt) {
}
