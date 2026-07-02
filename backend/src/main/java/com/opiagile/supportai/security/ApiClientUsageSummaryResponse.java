package com.opiagile.supportai.security;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApiClientUsageSummaryResponse(
        UUID apiClientId,
        String apiClientName,
        String tenantSlug,
        String workspaceSlug,
        long totalRequests,
        long allowedRequests,
        long blockedRequests,
        OffsetDateTime lastRequestAt) {
}
