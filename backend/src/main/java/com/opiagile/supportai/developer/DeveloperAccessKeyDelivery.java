package com.opiagile.supportai.developer;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

record DeveloperAccessKeyDelivery(
        UUID id,
        UUID requestId,
        UUID apiClientId,
        String encryptedApiKey,
        String keyPrefix,
        String tenantSlug,
        String workspaceSlug,
        Set<String> scopes,
        int rateLimitPerMinute,
        OffsetDateTime sandboxExpiresAt,
        OffsetDateTime deliveryExpiresAt,
        String retentionNotice) {
}
