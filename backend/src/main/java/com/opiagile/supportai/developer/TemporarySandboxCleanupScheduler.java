package com.opiagile.supportai.developer;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class TemporarySandboxCleanupScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemporarySandboxCleanupScheduler.class);

    private final TemporarySandboxCleanupService cleanupService;
    private final boolean enabled;

    public TemporarySandboxCleanupScheduler(
            TemporarySandboxCleanupService cleanupService,
            @Value("${developer-access.sandbox.cleanup.enabled:true}") boolean enabled) {
        this.cleanupService = cleanupService;
        this.enabled = enabled;
    }

    @Scheduled(fixedDelayString = "${developer-access.sandbox.cleanup.fixed-delay-ms:300000}")
    public void cleanupExpiredSandboxes() {
        if (!enabled) {
            return;
        }
        List<String> removed = cleanupService.cleanupExpiredSandboxes();
        if (!removed.isEmpty()) {
            LOGGER.info("Sandboxes temporários removidos: {}", removed);
        }
    }
}
