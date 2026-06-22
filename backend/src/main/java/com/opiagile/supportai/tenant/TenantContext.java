package com.opiagile.supportai.tenant;

import java.util.UUID;

public record TenantContext(
        UUID tenantId,
        String tenantSlug,
        UUID workspaceId,
        String workspaceSlug,
        String workspaceName) {
}
