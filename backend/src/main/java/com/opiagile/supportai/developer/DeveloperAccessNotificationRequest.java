package com.opiagile.supportai.developer;

import java.time.OffsetDateTime;
import java.util.UUID;

record DeveloperAccessNotificationRequest(
        UUID id,
        OffsetDateTime createdAt,
        String name,
        String company,
        String email,
        String useCase,
        String requestedResources) {
}
