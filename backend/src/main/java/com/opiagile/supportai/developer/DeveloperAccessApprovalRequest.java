package com.opiagile.supportai.developer;

import java.util.Set;

public record DeveloperAccessApprovalRequest(
        String tenantSlug,
        String workspaceSlug,
        Set<String> scopes,
        Integer rateLimitPerMinute,
        String clientName,
        Integer expiresInHours) {
}
