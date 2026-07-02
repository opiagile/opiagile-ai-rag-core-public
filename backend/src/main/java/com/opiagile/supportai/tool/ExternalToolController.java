package com.opiagile.supportai.tool;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.opiagile.supportai.security.DemoSecurityPolicy;
import com.opiagile.supportai.security.ApiClientContextHolder;
import com.opiagile.supportai.tenant.TenantContext;
import com.opiagile.supportai.tenant.TenantContextResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/tools")
public class ExternalToolController {

    private final ExternalToolRepository repository;
    private final SqlReadOnlyToolService sqlReadOnlyToolService;
    private final TenantContextResolver tenantContextResolver;
    private final DemoSecurityPolicy securityPolicy;
    private final boolean requireAdminToken;

    public ExternalToolController(
            ExternalToolRepository repository,
            SqlReadOnlyToolService sqlReadOnlyToolService,
            TenantContextResolver tenantContextResolver,
            DemoSecurityPolicy securityPolicy,
            @Value("${tools.execution.require-admin-token:true}") boolean requireAdminToken) {
        this.repository = repository;
        this.sqlReadOnlyToolService = sqlReadOnlyToolService;
        this.tenantContextResolver = tenantContextResolver;
        this.securityPolicy = securityPolicy;
        this.requireAdminToken = requireAdminToken;
    }

    @GetMapping
    public List<ExternalToolResponse> list(
            @RequestHeader(name = "X-Tenant-Id", required = false) String tenantId,
            @RequestHeader(name = "X-Workspace-Id", required = false) String workspaceId) {
        TenantContext tenantContext = tenantContextResolver.resolve(tenantId, workspaceId);
        return repository.findAll(tenantContext).stream()
                .map(ExternalToolResponse::from)
                .toList();
    }

    @PostMapping("/{slug}/sql/query")
    public SqlToolExecutionResponse executeSql(
            @RequestHeader(name = "X-Tenant-Id", required = false) String tenantId,
            @RequestHeader(name = "X-Workspace-Id", required = false) String workspaceId,
            @PathVariable String slug,
            @Valid @RequestBody SqlToolExecutionRequest request,
            HttpServletRequest httpRequest) {
        requireAdminAccess(httpRequest);
        return sqlReadOnlyToolService.execute(tenantContextResolver.resolve(tenantId, workspaceId), slug, request);
    }

    private void requireAdminAccess(HttpServletRequest request) {
        if (!requireAdminToken) {
            return;
        }
        boolean apiClientCanExecuteTools = ApiClientContextHolder.current()
                .map(context -> context.hasScope("tools:execute"))
                .orElse(false);
        if (!apiClientCanExecuteTools && !securityPolicy.adminTokenMatches(request)) {
            throw new ToolAccessDeniedException("Token administrativo obrigatório para executar ferramentas.");
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class ToolAccessDeniedException extends RuntimeException {
        ToolAccessDeniedException(String message) {
            super(message);
        }
    }
}
