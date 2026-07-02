package com.opiagile.supportai.tenant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.opiagile.supportai.security.ApiClientContextHolder;

@Component
public class TenantContextResolver {

    private final TenantRepository tenantRepository;
    private final String defaultTenant;
    private final String defaultWorkspace;

    public TenantContextResolver(
            TenantRepository tenantRepository,
            @Value("${tenant.default-tenant:demo}") String defaultTenant,
            @Value("${tenant.default-workspace:clinica-demo}") String defaultWorkspace) {
        this.tenantRepository = tenantRepository;
        this.defaultTenant = normalize(defaultTenant, "demo");
        this.defaultWorkspace = normalize(defaultWorkspace, "clinica-demo");
    }

    public TenantContext resolve(String tenantHeader, String workspaceHeader) {
        return ApiClientContextHolder.current()
                .map(context -> context.tenantContext())
                .orElseGet(() -> resolveFromHeaders(tenantHeader, workspaceHeader));
    }

    private TenantContext resolveFromHeaders(String tenantHeader, String workspaceHeader) {
        String tenantSlug = normalize(tenantHeader, defaultTenant);
        String workspaceSlug = normalize(workspaceHeader, defaultWorkspace);
        return tenantRepository.findContext(tenantSlug, workspaceSlug)
                .orElseThrow(() -> new IllegalArgumentException("Tenant/workspace não encontrado: " + tenantSlug + "/" + workspaceSlug));
    }

    public String resolveTenantSlug(String tenantHeader) {
        return ApiClientContextHolder.current()
                .map(context -> context.tenantContext().tenantSlug())
                .orElseGet(() -> normalize(tenantHeader, defaultTenant));
    }

    public TenantContext defaultContext() {
        return resolveFromHeaders(defaultTenant, defaultWorkspace);
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase();
    }
}
