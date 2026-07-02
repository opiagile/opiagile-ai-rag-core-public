package com.opiagile.supportai.chat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SiteChatProperties {

    private final boolean enabled;
    private final String apiKey;
    private final String tenant;
    private final String workspace;
    private final int rateLimitPerMinute;

    public SiteChatProperties(
            @Value("${site-chat.enabled:true}") boolean enabled,
            @Value("${site-chat.api-key:}") String apiKey,
            @Value("${site-chat.tenant:opiagile}") String tenant,
            @Value("${site-chat.workspace:opiagile-rag}") String workspace,
            @Value("${site-chat.rate-limit-per-minute:20}") int rateLimitPerMinute) {
        if (rateLimitPerMinute < 1) {
            throw new IllegalArgumentException("site-chat.rate-limit-per-minute deve ser maior ou igual a 1");
        }
        this.enabled = enabled;
        this.apiKey = normalize(apiKey);
        this.tenant = normalize(tenant);
        this.workspace = normalize(workspace);
        this.rateLimitPerMinute = rateLimitPerMinute;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean hasApiKey() {
        return !apiKey.isBlank();
    }

    public String apiKey() {
        return apiKey;
    }

    public String tenant() {
        return tenant;
    }

    public String workspace() {
        return workspace;
    }

    public int rateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
