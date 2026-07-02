package com.opiagile.supportai.developer;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.opiagile.supportai.security.DemoSecurityPolicy;
import com.opiagile.supportai.tenant.TenantRepository;
import com.opiagile.supportai.tenant.WorkspaceResponse;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;

@Hidden
@RestController
@RequestMapping("/api/admin/developer-access-requests")
public class DeveloperAccessAdminController {

    private final DeveloperAccessRequestRepository repository;
    private final DeveloperAccessApprovalService approvalService;
    private final DemoSecurityPolicy securityPolicy;
    private final TenantRepository tenantRepository;

    public DeveloperAccessAdminController(
            DeveloperAccessRequestRepository repository,
            DeveloperAccessApprovalService approvalService,
            DemoSecurityPolicy securityPolicy,
            TenantRepository tenantRepository) {
        this.repository = repository;
        this.approvalService = approvalService;
        this.securityPolicy = securityPolicy;
        this.tenantRepository = tenantRepository;
    }

    @GetMapping
    public List<DeveloperAccessRequestAdminResponse> list(
            @RequestParam(name = "status", defaultValue = "NEW") String status,
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            HttpServletRequest request) {
        requireAdminAccess(request);
        return repository.findForAdmin(normalizeStatus(status), Math.max(1, Math.min(limit, 200)));
    }

    @GetMapping("/workspaces")
    public List<DeveloperAccessWorkspaceOption> workspaces(
            @RequestParam(name = "tenant", defaultValue = "demo") String tenant,
            HttpServletRequest request) {
        requireAdminAccess(request);
        return tenantRepository.findWorkspaces(normalizeTenant(tenant)).stream()
                .map(this::workspaceOption)
                .toList();
    }

    @PostMapping("/{requestId}/approve")
    public DeveloperAccessApprovalResponse approve(
            @PathVariable UUID requestId,
            @RequestBody(required = false) DeveloperAccessApprovalRequest approvalRequest,
            HttpServletRequest request) {
        requireAdminAccess(request);
        return approvalService.approve(requestId, approvalRequest);
    }

    @PostMapping("/{requestId}/approve-temporary-sandbox")
    public DeveloperAccessApprovalResponse approveTemporarySandbox(
            @PathVariable UUID requestId,
            @RequestBody(required = false) DeveloperAccessApprovalRequest approvalRequest,
            HttpServletRequest request) {
        requireAdminAccess(request);
        return approvalService.approveTemporarySandbox(requestId, approvalRequest);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return null;
        }
        return status.trim().toUpperCase();
    }

    private String normalizeTenant(String tenant) {
        if (tenant == null || tenant.isBlank()) {
            return "demo";
        }
        return tenant.trim().toLowerCase();
    }

    private DeveloperAccessWorkspaceOption workspaceOption(WorkspaceResponse workspace) {
        return new DeveloperAccessWorkspaceOption(
                workspace.tenantSlug(),
                workspace.slug(),
                workspace.name(),
                workspace.description());
    }

    private void requireAdminAccess(HttpServletRequest request) {
        if (!securityPolicy.adminTokenMatches(request)) {
            throw new DeveloperAccessAdminAccessDeniedException("Token administrativo ausente ou inválido.");
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class DeveloperAccessAdminAccessDeniedException extends RuntimeException {
        DeveloperAccessAdminAccessDeniedException(String message) {
            super(message);
        }
    }
}
