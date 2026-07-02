package com.opiagile.supportai.developer;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DeveloperAccessResponse(
        UUID requestId,
        String status,
        String message,
        OffsetDateTime createdAt) {
}
