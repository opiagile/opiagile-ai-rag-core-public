package com.opiagile.supportai.security;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApiClientUsageLogRecord(
        UUID apiClientId,
        UUID tenantId,
        UUID workspaceId,
        String method,
        String path,
        String scope,
        int statusCode,
        boolean allowed,
        String blockedReason,
        String clientIp,
        String userAgent,
        int latencyMs) {
}
