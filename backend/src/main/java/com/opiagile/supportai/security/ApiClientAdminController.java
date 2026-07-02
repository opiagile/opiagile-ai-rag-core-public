package com.opiagile.supportai.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;

@Hidden
@RestController
@RequestMapping("/api/admin/api-clients")
public class ApiClientAdminController {

    private final ApiClientUsageLogRepository usageLogRepository;
    private final DemoSecurityPolicy securityPolicy;

    public ApiClientAdminController(ApiClientUsageLogRepository usageLogRepository, DemoSecurityPolicy securityPolicy) {
        this.usageLogRepository = usageLogRepository;
        this.securityPolicy = securityPolicy;
    }

    @GetMapping("/usage")
    public ApiClientUsageReportResponse usage(
            @RequestParam(name = "limit", defaultValue = "50") int limit,
            HttpServletRequest request) {
        requireAdminAccess(request);
        int normalizedLimit = Math.max(1, Math.min(limit, 200));
        return new ApiClientUsageReportResponse(
                usageLogRepository.summarizeByClient(normalizedLimit),
                usageLogRepository.findRecent(normalizedLimit));
    }

    private void requireAdminAccess(HttpServletRequest request) {
        if (!securityPolicy.adminTokenMatches(request)) {
            throw new ApiClientAdminAccessDeniedException("Token administrativo ausente ou inválido.");
        }
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    static class ApiClientAdminAccessDeniedException extends RuntimeException {
        ApiClientAdminAccessDeniedException(String message) {
            super(message);
        }
    }
}
