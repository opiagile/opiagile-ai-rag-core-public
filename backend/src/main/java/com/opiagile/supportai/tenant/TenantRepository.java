package com.opiagile.supportai.tenant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class TenantRepository {

    private final JdbcClient jdbc;

    public TenantRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<TenantContext> findContext(String tenantSlug, String workspaceSlug) {
        return jdbc.sql("""
                SELECT t.id AS tenant_id,
                       t.slug AS tenant_slug,
                       w.id AS workspace_id,
                       w.slug AS workspace_slug,
                       w.name AS workspace_name
                FROM tenants t
                JOIN workspaces w ON w.tenant_id = t.id
                WHERE t.slug = :tenantSlug
                  AND w.slug = :workspaceSlug
                """)
                .param("tenantSlug", tenantSlug)
                .param("workspaceSlug", workspaceSlug)
                .query(this::mapContext)
                .optional();
    }

    public List<WorkspaceResponse> findWorkspaces(String tenantSlug) {
        return jdbc.sql("""
                SELECT w.id, t.slug AS tenant_slug, w.slug, w.name, w.description
                FROM workspaces w
                JOIN tenants t ON t.id = w.tenant_id
                WHERE t.slug = :tenantSlug
                ORDER BY w.name
                """)
                .param("tenantSlug", tenantSlug)
                .query(this::mapWorkspace)
                .list();
    }

    public TenantContext createTemporarySandbox(
            String tenantSlug,
            String tenantName,
            String workspaceSlug,
            String workspaceName,
            String workspaceDescription,
            OffsetDateTime expiresAt) {
        UUID tenantId = jdbc.sql("""
                        INSERT INTO tenants (slug, name, sandbox, expires_at)
                        VALUES (:slug, :name, true, :expiresAt)
                        RETURNING id
                        """)
                .param("slug", tenantSlug)
                .param("name", tenantName)
                .param("expiresAt", expiresAt)
                .query(UUID.class)
                .single();

        UUID workspaceId = jdbc.sql("""
                        INSERT INTO workspaces (tenant_id, slug, name, description, sandbox, expires_at)
                        VALUES (:tenantId, :slug, :name, :description, true, :expiresAt)
                        RETURNING id
                        """)
                .param("tenantId", tenantId)
                .param("slug", workspaceSlug)
                .param("name", workspaceName)
                .param("description", workspaceDescription)
                .param("expiresAt", expiresAt)
                .query(UUID.class)
                .single();

        return new TenantContext(tenantId, tenantSlug, workspaceId, workspaceSlug, workspaceName);
    }

    public List<TemporarySandboxTenant> findExpiredTemporarySandboxes(int limit) {
        return jdbc.sql("""
                        SELECT id, slug, expires_at
                        FROM tenants
                        WHERE sandbox = true
                          AND expires_at IS NOT NULL
                          AND expires_at <= now()
                        ORDER BY expires_at ASC
                        LIMIT :limit
                        """)
                .param("limit", limit)
                .query((rs, rowNum) -> new TemporarySandboxTenant(
                        rs.getObject("id", UUID.class),
                        rs.getString("slug"),
                        rs.getObject("expires_at", OffsetDateTime.class)))
                .list();
    }

    public int deleteTemporarySandbox(UUID tenantId) {
        jdbc.sql("DELETE FROM retrieval_logs WHERE tenant_id = :tenantId")
                .param("tenantId", tenantId)
                .update();
        jdbc.sql("DELETE FROM documents WHERE tenant_id = :tenantId")
                .param("tenantId", tenantId)
                .update();
        jdbc.sql("DELETE FROM conversations WHERE tenant_id = :tenantId")
                .param("tenantId", tenantId)
                .update();
        return jdbc.sql("DELETE FROM tenants WHERE id = :tenantId AND sandbox = true")
                .param("tenantId", tenantId)
                .update();
    }

    private TenantContext mapContext(ResultSet rs, int rowNum) throws SQLException {
        return new TenantContext(
                rs.getObject("tenant_id", UUID.class),
                rs.getString("tenant_slug"),
                rs.getObject("workspace_id", UUID.class),
                rs.getString("workspace_slug"),
                rs.getString("workspace_name"));
    }

    private WorkspaceResponse mapWorkspace(ResultSet rs, int rowNum) throws SQLException {
        return new WorkspaceResponse(
                rs.getObject("id", UUID.class),
                rs.getString("tenant_slug"),
                rs.getString("slug"),
                rs.getString("name"),
                rs.getString("description"));
    }
}
