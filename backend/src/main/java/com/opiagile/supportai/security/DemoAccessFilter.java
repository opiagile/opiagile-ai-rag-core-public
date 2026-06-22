package com.opiagile.supportai.security;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opiagile.supportai.common.ErrorResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class DemoAccessFilter extends OncePerRequestFilter {

    private final DemoSecurityPolicy policy;
    private final SimpleRateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public DemoAccessFilter(DemoSecurityPolicy policy, SimpleRateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.policy = policy;
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (isDemoWriteEndpoint(request) && !policy.accessTokenMatches(request)) {
            writeError(response, HttpStatus.UNAUTHORIZED, "TOKEN_DEMO_INVALIDO", "Token da demo ausente ou inválido.");
            return;
        }
        if (policy.rateLimitEnabled() && isRateLimitedEndpoint(request)) {
            int limit = isUpload(request) ? policy.uploadRateLimitPerMinute() : policy.chatRateLimitPerMinute();
            String key = endpointName(request) + ":" + clientIp(request);
            if (!rateLimiter.allow(key, limit)) {
                writeError(response, HttpStatus.TOO_MANY_REQUESTS, "LIMITE_DEMO_EXCEDIDO", "Limite de uso da demo excedido. Aguarde um minuto e tente novamente.");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isDemoWriteEndpoint(HttpServletRequest request) {
        return isChat(request) || isUpload(request);
    }

    private boolean isRateLimitedEndpoint(HttpServletRequest request) {
        return isChat(request) || isUpload(request);
    }

    private boolean isChat(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && "/api/chat".equals(request.getRequestURI());
    }

    private boolean isUpload(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && "/api/documents/upload".equals(request.getRequestURI());
    }

    private String endpointName(HttpServletRequest request) {
        return isUpload(request) ? "upload" : "chat";
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), new ErrorResponse(code, message, OffsetDateTime.now()));
    }
}
