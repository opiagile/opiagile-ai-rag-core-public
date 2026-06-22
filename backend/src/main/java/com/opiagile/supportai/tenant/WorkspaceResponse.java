package com.opiagile.supportai.tenant;

import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String tenantSlug,
        String slug,
        String name,
        String description) {
}
