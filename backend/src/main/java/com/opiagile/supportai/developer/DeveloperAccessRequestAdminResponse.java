package com.opiagile.supportai.developer;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeveloperAccessRequestAdminResponse(
        UUID id,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String name,
        String company,
        String email,
        String useCase,
        String requestedResources,
        String source,
        String status,
        boolean notificationEmailSent,
        int notificationEmailAttempts,
        String notificationEmailLastError,
        String approvedTenantSlug,
        String approvedWorkspaceSlug,
        OffsetDateTime sandboxExpiresAt,
        OffsetDateTime sandboxDeletedAt) {
}
