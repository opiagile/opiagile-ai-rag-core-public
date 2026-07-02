package com.opiagile.supportai.developer;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.opiagile.supportai.security.ApiClientRepository;
import com.opiagile.supportai.security.ApiKeyHasher;
import com.opiagile.supportai.tenant.TenantContext;
import com.opiagile.supportai.tenant.TenantRepository;

@Service
public class DeveloperAccessApprovalService {

    private static final Set<String> DEFAULT_SCOPES = Set.of(
            "chat:write",
            "documents:upload",
            "documents:read",
            "providers:read",
            "workspaces:read");
    private static final int DEFAULT_RATE_LIMIT = 30;
    private static final int MAX_RATE_LIMIT = 120;
    private static final int DEFAULT_SANDBOX_HOURS = 24;
    private static final int MAX_SANDBOX_HOURS = 168;

    private final DeveloperAccessRequestRepository accessRequestRepository;
    private final ApiClientRepository apiClientRepository;
    private final TenantRepository tenantRepository;
    private final DeveloperAccessKeyDeliveryService keyDeliveryService;
    private final DeveloperAccessEmailTemplateService emailTemplateService;
    private final SecureRandom secureRandom = new SecureRandom();

    public DeveloperAccessApprovalService(
            DeveloperAccessRequestRepository accessRequestRepository,
            ApiClientRepository apiClientRepository,
            TenantRepository tenantRepository,
            DeveloperAccessKeyDeliveryService keyDeliveryService,
            DeveloperAccessEmailTemplateService emailTemplateService) {
        this.accessRequestRepository = accessRequestRepository;
        this.apiClientRepository = apiClientRepository;
        this.tenantRepository = tenantRepository;
        this.keyDeliveryService = keyDeliveryService;
        this.emailTemplateService = emailTemplateService;
    }

    @Transactional
    public DeveloperAccessApprovalResponse approve(UUID requestId, DeveloperAccessApprovalRequest approvalRequest) {
        DeveloperAccessRequestAdminResponse request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação developer não encontrada."));
        if ("APPROVED".equalsIgnoreCase(request.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solicitação developer já aprovada. A chave não pode ser reemitida.");
        }

        String tenantSlug = normalize(approvalRequest == null ? null : approvalRequest.tenantSlug(), "demo");
        String workspaceSlug = normalize(approvalRequest == null ? null : approvalRequest.workspaceSlug(), "clinica-demo");
        TenantContext tenantContext = tenantRepository.findContext(tenantSlug, workspaceSlug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant/workspace não encontrado."));

        Set<String> scopes = normalizeScopes(approvalRequest == null ? null : approvalRequest.scopes());
        int rateLimit = normalizeRateLimit(approvalRequest == null ? null : approvalRequest.rateLimitPerMinute());
        String apiKey = generateApiKey();
        String keyPrefix = apiKey.substring(0, Math.min(apiKey.length(), 24));
        String clientName = clientName(approvalRequest, request);

        UUID apiClientId = apiClientRepository.create(
                tenantContext,
                clientName,
                keyPrefix,
                ApiKeyHasher.sha256Hex(apiKey),
                scopes,
                rateLimit);
        if (!accessRequestRepository.markApproved(requestId, apiClientId, tenantContext.tenantSlug(), tenantContext.workspaceSlug(), null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solicitação developer já foi alterada por outro processo.");
        }

        DeveloperAccessApprovalResponse response = response(
                requestId,
                apiClientId,
                apiKey,
                keyPrefix,
                tenantContext.tenantSlug(),
                tenantContext.workspaceSlug(),
                scopes,
                rateLimit,
                null,
                null,
                "Copie esta API key agora. Ela não fica armazenada em texto claro e não poderá ser exibida novamente.");
        emailTemplateService.enqueueSandboxApproved(request, response, response.keyDeliveryUrl(), response.keyDeliveryExpiresAt());
        return response;
    }

    @Transactional
    public DeveloperAccessApprovalResponse approveTemporarySandbox(UUID requestId, DeveloperAccessApprovalRequest approvalRequest) {
        DeveloperAccessRequestAdminResponse request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitação developer não encontrada."));
        if ("APPROVED".equalsIgnoreCase(request.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solicitação developer já aprovada. A chave não pode ser reemitida.");
        }

        int expiresInHours = normalizeSandboxHours(approvalRequest == null ? null : approvalRequest.expiresInHours());
        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(expiresInHours);
        String suffix = requestId.toString().substring(0, 8);
        String clientBase = clientBase(request);
        String tenantSlug = truncateSlug("sandbox-" + clientBase + "-" + suffix, 80);
        String workspaceSlug = truncateSlug(clientBase + "-sandbox-" + suffix, 80);
        String displayName = clientDisplayName(request);
        TenantContext tenantContext = tenantRepository.createTemporarySandbox(
                tenantSlug,
                "Sandbox - " + displayName,
                workspaceSlug,
                displayName + " - Sandbox",
                "Workspace sandbox temporário para validação controlada da API Opiagile. Dados enviados neste sandbox são removidos quando o ambiente expira.",
                expiresAt);

        Set<String> scopes = normalizeScopes(approvalRequest == null ? null : approvalRequest.scopes());
        int rateLimit = normalizeRateLimit(approvalRequest == null ? null : approvalRequest.rateLimitPerMinute());
        String apiKey = generateApiKey();
        String keyPrefix = apiKey.substring(0, Math.min(apiKey.length(), 24));
        String clientName = clientName(approvalRequest, request);

        UUID apiClientId = apiClientRepository.create(
                tenantContext,
                clientName,
                keyPrefix,
                ApiKeyHasher.sha256Hex(apiKey),
                scopes,
                rateLimit,
                expiresAt);
        if (!accessRequestRepository.markApproved(requestId, apiClientId, tenantContext.tenantSlug(), tenantContext.workspaceSlug(), expiresAt)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solicitação developer já foi alterada por outro processo.");
        }

        DeveloperAccessApprovalResponse response = response(
                requestId,
                apiClientId,
                apiKey,
                keyPrefix,
                tenantContext.tenantSlug(),
                tenantContext.workspaceSlug(),
                scopes,
                rateLimit,
                expiresAt,
                retentionNotice(),
                "Copie esta API key agora. Ela expira junto com o sandbox temporário e não poderá ser exibida novamente.");
        emailTemplateService.enqueueSandboxApproved(request, response, response.keyDeliveryUrl(), response.keyDeliveryExpiresAt());
        return response;
    }

    private DeveloperAccessApprovalResponse response(
            UUID requestId,
            UUID apiClientId,
            String apiKey,
            String keyPrefix,
            String tenantSlug,
            String workspaceSlug,
            Set<String> scopes,
            int rateLimit,
            OffsetDateTime expiresAt,
            String retentionNotice,
            String warning) {
        DeveloperAccessKeyDeliveryLink deliveryLink = keyDeliveryService.createLink(
                requestId,
                apiClientId,
                apiKey,
                keyPrefix,
                tenantSlug,
                workspaceSlug,
                scopes,
                rateLimit,
                expiresAt,
                retentionNotice);
        return new DeveloperAccessApprovalResponse(
                requestId,
                apiClientId,
                apiKey,
                keyPrefix,
                tenantSlug,
                workspaceSlug,
                scopes,
                rateLimit,
                OffsetDateTime.now(),
                expiresAt,
                deliveryLink.url(),
                deliveryLink.deliveryExpiresAt(),
                retentionNotice,
                warning);
    }

    private String clientName(DeveloperAccessApprovalRequest approvalRequest, DeveloperAccessRequestAdminResponse request) {
        String requested = approvalRequest == null ? "" : normalizeText(approvalRequest.clientName());
        if (!requested.isBlank()) {
            return truncate(requested, 160);
        }
        String company = normalizeText(request.company());
        if (!company.isBlank()) {
            return truncate("Sandbox - " + company, 160);
        }
        return truncate("Sandbox - " + request.email(), 160);
    }

    private Set<String> normalizeScopes(Set<String> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return DEFAULT_SCOPES;
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String scope : scopes) {
            String value = normalize(scope, "");
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return normalized.isEmpty() ? DEFAULT_SCOPES : Set.copyOf(normalized);
    }

    private int normalizeRateLimit(Integer requested) {
        if (requested == null) {
            return DEFAULT_RATE_LIMIT;
        }
        return Math.max(1, Math.min(requested, MAX_RATE_LIMIT));
    }

    private int normalizeSandboxHours(Integer requested) {
        if (requested == null) {
            return DEFAULT_SANDBOX_HOURS;
        }
        return Math.max(1, Math.min(requested, MAX_SANDBOX_HOURS));
    }

    private String generateApiKey() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return "opiagile_sandbox_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase();
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String clientDisplayName(DeveloperAccessRequestAdminResponse request) {
        String company = normalizeText(request.company());
        if (!company.isBlank()) {
            return truncate(company, 120);
        }
        return truncate(request.name(), 120);
    }

    private String clientBase(DeveloperAccessRequestAdminResponse request) {
        String normalized = Normalizer.normalize(clientDisplayName(request), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            return "cliente";
        }
        return truncateSlug(normalized, 42);
    }

    private String truncateSlug(String value, int maxLength) {
        String normalized = value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9-]+", "-").replaceAll("(^-|-$)", "");
        if (normalized.isBlank()) {
            return "sandbox";
        }
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength).replaceAll("-$", "");
    }

    private String retentionNotice() {
        return "Sandbox temporário: após a expiração, o tenant/workspace e os dados enviados para teste são excluídos do ambiente sandbox. A solicitação do lead permanece registrada para contato, auditoria operacional e continuidade comercial, conforme práticas de LGPD.";
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
