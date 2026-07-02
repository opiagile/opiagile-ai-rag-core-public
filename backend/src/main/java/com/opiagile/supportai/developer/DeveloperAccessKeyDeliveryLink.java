package com.opiagile.supportai.developer;

import java.time.OffsetDateTime;
import java.util.UUID;

record DeveloperAccessKeyDeliveryLink(
        UUID id,
        String token,
        String url,
        OffsetDateTime deliveryExpiresAt) {
}
