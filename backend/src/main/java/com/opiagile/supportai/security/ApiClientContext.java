package com.opiagile.supportai.security;

import java.util.Set;
import java.util.UUID;

import com.opiagile.supportai.tenant.TenantContext;

public record ApiClientContext(
        UUID clientId,
        String name,
        TenantContext tenantContext,
        Set<String> scopes,
        int rateLimitPerMinute) {

    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }
}
