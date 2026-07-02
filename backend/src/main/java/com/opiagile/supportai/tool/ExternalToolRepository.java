package com.opiagile.supportai.tool;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.opiagile.supportai.tenant.TenantContext;

@Repository
public class ExternalToolRepository {

    private final JdbcClient jdbc;

    public ExternalToolRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<ExternalToolRecord> findAll(TenantContext tenantContext) {
        return jdbc.sql("""
                SELECT id, tenant_id, workspace_id, slug, name, type, status, description,
                       ARRAY(SELECT jsonb_array_elements_text(config -> 'allowedTables')) AS allowed_tables,
                       COALESCE((config ->> 'defaultLimit')::int, 20) AS default_limit,
                       COALESCE((config ->> 'maxLimit')::int, 50) AS max_limit
                FROM external_tools
                WHERE tenant_id = :tenantId
                  AND workspace_id = :workspaceId
                ORDER BY name
                """)
                .param("tenantId", tenantContext.tenantId())
                .param("workspaceId", tenantContext.workspaceId())
                .query(this::mapTool)
                .list();
    }

    public Optional<ExternalToolRecord> findActive(TenantContext tenantContext, String slug) {
        return jdbc.sql("""
                SELECT id, tenant_id, workspace_id, slug, name, type, status, description,
                       ARRAY(SELECT jsonb_array_elements_text(config -> 'allowedTables')) AS allowed_tables,
                       COALESCE((config ->> 'defaultLimit')::int, 20) AS default_limit,
                       COALESCE((config ->> 'maxLimit')::int, 50) AS max_limit
                FROM external_tools
                WHERE tenant_id = :tenantId
                  AND workspace_id = :workspaceId
                  AND slug = :slug
                  AND status = 'ACTIVE'
                """)
                .param("tenantId", tenantContext.tenantId())
                .param("workspaceId", tenantContext.workspaceId())
                .param("slug", slug)
                .query(this::mapTool)
                .optional();
    }

    public void saveExecutionLog(
            TenantContext tenantContext,
            ExternalToolRecord tool,
            String status,
            String queryPreview,
            int rowCount,
            int latencyMs,
            String errorMessage) {
        jdbc.sql("""
                INSERT INTO external_tool_execution_logs
                    (tenant_id, workspace_id, tool_id, tool_slug, status, query_preview, row_count, latency_ms, error_message)
                VALUES
                    (:tenantId, :workspaceId, :toolId, :toolSlug, :status, :queryPreview, :rowCount, :latencyMs, :errorMessage)
                """)
                .param("tenantId", tenantContext.tenantId())
                .param("workspaceId", tenantContext.workspaceId())
                .param("toolId", tool.id())
                .param("toolSlug", tool.slug())
                .param("status", status)
                .param("queryPreview", queryPreview)
                .param("rowCount", rowCount)
                .param("latencyMs", latencyMs)
                .param("errorMessage", errorMessage)
                .update();
    }

    private ExternalToolRecord mapTool(ResultSet rs, int rowNum) throws SQLException {
        return new ExternalToolRecord(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("workspace_id", UUID.class),
                rs.getString("slug"),
                rs.getString("name"),
                rs.getString("type"),
                rs.getString("status"),
                rs.getString("description"),
                stringArray(rs.getArray("allowed_tables")),
                rs.getInt("default_limit"),
                rs.getInt("max_limit"));
    }

    private List<String> stringArray(Array array) throws SQLException {
        if (array == null) {
            return List.of();
        }
        Object raw = array.getArray();
        if (raw instanceof String[] values) {
            return Arrays.asList(values);
        }
        return List.of();
    }
}
