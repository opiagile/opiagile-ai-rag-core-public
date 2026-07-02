package com.opiagile.supportai.developer;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

public record DeveloperAccessApprovalResponse(
        UUID requestId,
        UUID apiClientId,
        String apiKey,
        String keyPrefix,
        String tenantSlug,
        String workspaceSlug,
        Set<String> scopes,
        int rateLimitPerMinute,
        OffsetDateTime approvedAt,
        OffsetDateTime expiresAt,
        String keyDeliveryUrl,
        OffsetDateTime keyDeliveryExpiresAt,
        String retentionNotice,
        String warning) {
}
