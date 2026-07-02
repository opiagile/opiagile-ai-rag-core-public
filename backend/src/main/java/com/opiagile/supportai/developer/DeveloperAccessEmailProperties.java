package com.opiagile.supportai.developer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeveloperAccessEmailProperties {

    private final boolean enabled;
    private final String to;
    private final String from;
    private final String subjectPrefix;
    private final String publicBaseUrl;
    private final int keyDeliveryExpiresInHours;

    public DeveloperAccessEmailProperties(
            @Value("${developer-access.email.enabled:false}") boolean enabled,
            @Value("${developer-access.email.to:contato@opiagile.com}") String to,
            @Value("${developer-access.email.from:contato@opiagile.com}") String from,
            @Value("${developer-access.email.subject-prefix:[Opiagile] Solicitação de API key sandbox}") String subjectPrefix,
            @Value("${developer-access.email.public-base-url:https://opiagile.com}") String publicBaseUrl,
            @Value("${developer-access.email.key-delivery-expires-in-hours:24}") int keyDeliveryExpiresInHours) {
        this.enabled = enabled;
        this.to = normalize(to);
        this.from = normalize(from);
        this.subjectPrefix = normalize(subjectPrefix);
        this.publicBaseUrl = normalizePublicBaseUrl(publicBaseUrl);
        this.keyDeliveryExpiresInHours = Math.max(1, Math.min(keyDeliveryExpiresInHours, 168));
    }

    boolean enabled() {
        return enabled;
    }

    String to() {
        return to;
    }

    String from() {
        return from;
    }

    String subjectPrefix() {
        return subjectPrefix.isBlank() ? "[Opiagile] Solicitação de API key sandbox" : subjectPrefix;
    }

    String publicBaseUrl() {
        return publicBaseUrl.isBlank() ? "https://opiagile.com" : publicBaseUrl;
    }

    int keyDeliveryExpiresInHours() {
        return keyDeliveryExpiresInHours;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizePublicBaseUrl(String value) {
        String normalized = normalize(value);
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
