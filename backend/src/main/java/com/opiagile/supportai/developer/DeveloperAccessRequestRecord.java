package com.opiagile.supportai.developer;

import java.time.OffsetDateTime;
import java.util.UUID;

record DeveloperAccessRequestRecord(
        UUID id,
        OffsetDateTime createdAt) {
}
