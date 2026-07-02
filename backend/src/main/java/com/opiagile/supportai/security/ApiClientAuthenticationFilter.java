package com.opiagile.supportai.security;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import tools.jackson.databind.ObjectMapper;
import com.opiagile.supportai.common.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiClientAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-OPIAGILE-API-KEY";

    private final ApiSecurityProperties properties;
    private final ApiClientRepository repository;
    private final ApiClientUsageLogRepository usageLogRepository;
    private final SimpleRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public ApiClientAuthenticationFilter(
            ApiSecurityProperties properties,
            ApiClientRepository repository,
            ApiClientUsageLogRepository usageLogRepository,
            SimpleRateLimiter rateLimiter,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.repository = repository;
        this.usageLogRepository = usageLogRepository;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!properties.enabled() || !isProtectedEndpoint(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<String> apiKey = apiKey(request);
        if (apiKey.isEmpty()) {
            if (properties.requireApiKey()) {
                audit(request, null, requiredScope(request), HttpStatus.UNAUTHORIZED.value(), false, "API_KEY_AUSENTE", 0);
                writeError(response, HttpStatus.UNAUTHORIZED, "API_KEY_AUSENTE", "Chave de API ausente.");
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }

        Optional<ApiClientRecord> client = repository.findByKeyHash(ApiKeyHasher.sha256Hex(apiKey.get()));
        if (client.isEmpty() || !client.get().active()) {
            audit(request, null, requiredScope(request), HttpStatus.UNAUTHORIZED.value(), false, "API_KEY_INVALIDA", 0);
            writeError(response, HttpStatus.UNAUTHORIZED, "API_KEY_INVALIDA", "Chave de API ausente, inválida ou inativa.");
            return;
        }

        String requiredScope = requiredScope(request);
        ApiClientContext context = client.get().toContext();
        if (!requiredScope.isBlank() && !context.hasScope(requiredScope)) {
            audit(request, context, requiredScope, HttpStatus.FORBIDDEN.value(), false, "ESCOPO_INSUFICIENTE", 0);
            writeError(response, HttpStatus.FORBIDDEN, "ESCOPO_INSUFICIENTE", "A chave de API não possui permissão para esta operação.");
            return;
        }

        int limit = context.rateLimitPerMinute() > 0 ? context.rateLimitPerMinute() : properties.defaultRateLimitPerMinute();
        if (!rateLimiter.allow("api-client:" + context.clientId(), limit)) {
            audit(request, context, requiredScope, HttpStatus.TOO_MANY_REQUESTS.value(), false, "LIMITE_EXCEDIDO", 0);
            writeError(response, HttpStatus.TOO_MANY_REQUESTS, "LIMITE_API_CLIENT_EXCEDIDO", "Limite de uso da chave de API excedido. Aguarde um minuto e tente novamente.");
            return;
        }

        long startedAt = System.nanoTime();
        try {
            ApiClientContextHolder.set(context);
            repository.markUsed(context.clientId());
            filterChain.doFilter(request, response);
            audit(request, context, requiredScope, response.getStatus(), response.getStatus() < 400, null, elapsedMs(startedAt));
        } finally {
            ApiClientContextHolder.clear();
        }
    }

    private boolean isProtectedEndpoint(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/api/chat")
                || path.startsWith("/api/documents")
                || path.startsWith("/api/conversations")
                || path.startsWith("/api/observability")
                || path.startsWith("/api/handoffs")
                || path.startsWith("/api/tools")
                || path.startsWith("/api/providers")
                || path.startsWith("/api/workspaces");
    }

    private String requiredScope(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equalsIgnoreCase(method) && path.equals("/api/chat")) {
            return "chat:write";
        }
        if ("POST".equalsIgnoreCase(method) && path.equals("/api/documents/upload")) {
            return "documents:upload";
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/documents")) {
            return "documents:read";
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/conversations")) {
            return "conversations:read";
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/observability")) {
            return "observability:read";
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/handoffs")) {
            return "handoffs:read";
        }
        if (("PATCH".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method)) && path.startsWith("/api/handoffs")) {
            return "handoffs:write";
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/tools")) {
            return "tools:read";
        }
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/tools/")) {
            return "tools:execute";
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/providers")) {
            return "providers:read";
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/workspaces")) {
            return "workspaces:read";
        }
        return "";
    }

    private Optional<String> apiKey(HttpServletRequest request) {
        String header = normalize(request.getHeader(API_KEY_HEADER));
        if (!header.isBlank()) {
            return Optional.of(header);
        }
        String authorization = normalize(request.getHeader("Authorization"));
        String prefix = "Bearer ";
        if (authorization.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return Optional.of(normalize(authorization.substring(prefix.length())));
        }
        return Optional.empty();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private int elapsedMs(long startedAt) {
        return Math.max(0, (int) ((System.nanoTime() - startedAt) / 1_000_000));
    }

    private void audit(
            HttpServletRequest request,
            ApiClientContext context,
            String scope,
            int statusCode,
            boolean allowed,
            String blockedReason,
            int latencyMs) {
        try {
            usageLogRepository.save(new ApiClientUsageLogRecord(
                    context == null ? null : context.clientId(),
                    context == null ? null : context.tenantContext().tenantId(),
                    context == null ? null : context.tenantContext().workspaceId(),
                    request.getMethod(),
                    request.getRequestURI(),
                    scope == null || scope.isBlank() ? null : scope,
                    statusCode,
                    allowed,
                    blockedReason,
                    clientIp(request),
                    truncate(normalize(request.getHeader("User-Agent")), 180),
                    latencyMs));
        } catch (RuntimeException ignored) {
            // Auditoria não deve derrubar o tráfego principal.
        }
    }

    private String clientIp(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return truncate(realIp.trim(), 80);
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return truncate(forwardedFor.split(",")[0].trim(), 80);
        }
        return truncate(request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr(), 80);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(code, message, OffsetDateTime.now()));
    }
}
