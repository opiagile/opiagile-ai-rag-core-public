package com.opiagile.supportai.document;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentRecord(
        UUID id,
        String filename,
        String contentType,
        String sourceType,
        String status,
        OffsetDateTime createdAt) {
}
