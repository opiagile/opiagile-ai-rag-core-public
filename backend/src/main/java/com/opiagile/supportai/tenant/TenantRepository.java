package com.opiagile.supportai.tenant;

import java.sql.ResultSet;
import java.sql.SQLException;
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
