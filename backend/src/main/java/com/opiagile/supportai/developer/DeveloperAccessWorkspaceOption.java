package com.opiagile.supportai.developer;

public record DeveloperAccessWorkspaceOption(
        String tenantSlug,
        String workspaceSlug,
        String workspaceName,
        String description) {
}
