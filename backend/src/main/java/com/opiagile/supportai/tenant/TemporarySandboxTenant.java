package com.opiagile.supportai.tenant;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TemporarySandboxTenant(
        UUID tenantId,
        String tenantSlug,
        OffsetDateTime expiresAt) {
}
