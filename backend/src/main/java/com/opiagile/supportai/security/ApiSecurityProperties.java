package com.opiagile.supportai.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiSecurityProperties {

    private final boolean enabled;
    private final boolean requireApiKey;
    private final int defaultRateLimitPerMinute;

    public ApiSecurityProperties(
            @Value("${api-security.enabled:true}") boolean enabled,
            @Value("${api-security.require-api-key:false}") boolean requireApiKey,
            @Value("${api-security.default-rate-limit-per-minute:60}") int defaultRateLimitPerMinute) {
        if (defaultRateLimitPerMinute < 1) {
            throw new IllegalArgumentException("api-security.default-rate-limit-per-minute deve ser maior ou igual a 1");
        }
        this.enabled = enabled;
        this.requireApiKey = requireApiKey;
        this.defaultRateLimitPerMinute = defaultRateLimitPerMinute;
    }

    public boolean enabled() {
        return enabled;
    }

    public boolean requireApiKey() {
        return requireApiKey;
    }

    public int defaultRateLimitPerMinute() {
        return defaultRateLimitPerMinute;
    }
}
