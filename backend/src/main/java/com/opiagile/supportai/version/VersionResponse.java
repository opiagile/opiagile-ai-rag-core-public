package com.opiagile.supportai.version;

import java.time.OffsetDateTime;

public record VersionResponse(
        String appName,
        String version,
        String environment,
        String javaVersion,
        OffsetDateTime timestamp) {
}
