package com.opiagile.supportai.security;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

import com.opiagile.supportai.tenant.TenantContext;

public record ApiClientRecord(
        UUID id,
        String name,
        String keyPrefix,
        String keyHash,
        String status,
        Set<String> scopes,
        int rateLimitPerMinute,
        OffsetDateTime expiresAt,
        TenantContext tenantContext) {

    public boolean active() {
        return "ACTIVE".equalsIgnoreCase(status)
                && (expiresAt == null || expiresAt.isAfter(OffsetDateTime.now()));
    }

    public ApiClientContext toContext() {
        return new ApiClientContext(id, name, tenantContext, scopes, rateLimitPerMinute);
    }
}
