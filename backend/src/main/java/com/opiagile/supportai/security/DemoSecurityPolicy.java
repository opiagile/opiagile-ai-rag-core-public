package com.opiagile.supportai.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class DemoSecurityPolicy {

    private final String accessToken;
    private final String adminToken;
    private final boolean rateLimitEnabled;
    private final int chatRateLimitPerMinute;
    private final int uploadRateLimitPerMinute;

    public DemoSecurityPolicy(
            @Value("${demo.security.access-token:}") String accessToken,
            @Value("${demo.security.admin-token:}") String adminToken,
            @Value("${demo.security.rate-limit.enabled:true}") boolean rateLimitEnabled,
            @Value("${demo.security.rate-limit.chat-per-minute:30}") int chatRateLimitPerMinute,
            @Value("${demo.security.rate-limit.upload-per-minute:5}") int uploadRateLimitPerMinute) {
        if (chatRateLimitPerMinute < 1) {
            throw new IllegalArgumentException("demo.security.rate-limit.chat-per-minute deve ser maior ou igual a 1");
        }
        if (uploadRateLimitPerMinute < 1) {
            throw new IllegalArgumentException("demo.security.rate-limit.upload-per-minute deve ser maior ou igual a 1");
        }
        this.accessToken = normalize(accessToken);
        this.adminToken = normalize(adminToken);
        this.rateLimitEnabled = rateLimitEnabled;
        this.chatRateLimitPerMinute = chatRateLimitPerMinute;
        this.uploadRateLimitPerMinute = uploadRateLimitPerMinute;
    }

    public boolean requiresAccessToken() {
        return !accessToken.isBlank();
    }

    public boolean accessTokenMatches(HttpServletRequest request) {
        return !requiresAccessToken() || tokenMatches(request, accessToken);
    }

    public boolean adminResetEnabled() {
        return !adminToken.isBlank();
    }

    public boolean adminTokenMatches(HttpServletRequest request) {
        return adminResetEnabled() && tokenMatches(request, adminToken);
    }

    public boolean rateLimitEnabled() {
        return rateLimitEnabled;
    }

    public int chatRateLimitPerMinute() {
        return chatRateLimitPerMinute;
    }

    public int uploadRateLimitPerMinute() {
        return uploadRateLimitPerMinute;
    }

    private boolean tokenMatches(HttpServletRequest request, String expectedToken) {
        String demoToken = normalize(request.getHeader("X-Demo-Token"));
        String adminTokenHeader = normalize(request.getHeader("X-Demo-Admin-Token"));
        String bearerToken = bearerToken(request.getHeader("Authorization"));
        return expectedToken.equals(demoToken)
                || expectedToken.equals(adminTokenHeader)
                || expectedToken.equals(bearerToken);
    }

    private String bearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return "";
        }
        String prefix = "Bearer ";
        if (!authorization.regionMatches(true, 0, prefix, 0, prefix.length())) {
            return "";
        }
        return normalize(authorization.substring(prefix.length()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
