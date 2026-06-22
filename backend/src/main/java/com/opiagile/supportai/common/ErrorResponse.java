package com.opiagile.supportai.common;

import java.time.OffsetDateTime;

public record ErrorResponse(
        String error,
        String message,
        OffsetDateTime timestamp) {
}
