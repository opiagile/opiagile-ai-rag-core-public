package com.opiagile.supportai.tenant;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces")
public class WorkspaceController {

    private final TenantRepository tenantRepository;
    private final TenantContextResolver tenantContextResolver;

    public WorkspaceController(TenantRepository tenantRepository, TenantContextResolver tenantContextResolver) {
        this.tenantRepository = tenantRepository;
        this.tenantContextResolver = tenantContextResolver;
    }

    @GetMapping
    public List<WorkspaceResponse> findWorkspaces(@RequestHeader(name = "X-Tenant-Id", required = false) String tenantHeader) {
        return tenantRepository.findWorkspaces(tenantContextResolver.resolveTenantSlug(tenantHeader));
    }
}
