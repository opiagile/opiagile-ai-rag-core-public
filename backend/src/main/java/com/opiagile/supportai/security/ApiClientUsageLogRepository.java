package com.opiagile.supportai.security;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ApiClientUsageLogRepository {

    private final JdbcClient jdbc;

    public ApiClientUsageLogRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void save(ApiClientUsageLogRecord record) {
        jdbc.sql("""
                INSERT INTO api_client_usage_logs (
                    api_client_id,
                    tenant_id,
                    workspace_id,
                    method,
                    path,
                    scope,
                    status_code,
                    allowed,
                    blocked_reason,
                    client_ip,
                    user_agent,
                    latency_ms
                )
                VALUES (
                    :apiClientId,
                    :tenantId,
                    :workspaceId,
                    :method,
                    :path,
                    :scope,
                    :statusCode,
                    :allowed,
                    :blockedReason,
                    :clientIp,
                    :userAgent,
                    :latencyMs
                )
                """)
                .param("apiClientId", record.apiClientId())
                .param("tenantId", record.tenantId())
                .param("workspaceId", record.workspaceId())
                .param("method", record.method())
                .param("path", record.path())
                .param("scope", record.scope())
                .param("statusCode", record.statusCode())
                .param("allowed", record.allowed())
                .param("blockedReason", record.blockedReason())
                .param("clientIp", record.clientIp())
                .param("userAgent", record.userAgent())
                .param("latencyMs", record.latencyMs())
                .update();
    }

    public List<ApiClientUsageLogEntryResponse> findRecent(int limit) {
        return jdbc.sql("""
                SELECT l.id,
                       l.api_client_id,
                       c.name AS api_client_name,
                       t.slug AS tenant_slug,
                       w.slug AS workspace_slug,
                       l.method,
                       l.path,
                       l.scope,
                       l.status_code,
                       l.allowed,
                       l.blocked_reason,
                       l.client_ip,
                       l.user_agent,
                       l.latency_ms,
                       l.created_at
                FROM api_client_usage_logs l
                LEFT JOIN api_clients c ON c.id = l.api_client_id
                LEFT JOIN tenants t ON t.id = l.tenant_id
                LEFT JOIN workspaces w ON w.id = l.workspace_id
                ORDER BY l.created_at DESC
                LIMIT :limit
                """)
                .param("limit", limit)
                .query(this::mapEntry)
                .list();
    }

    public List<ApiClientUsageSummaryResponse> summarizeByClient(int limit) {
        return jdbc.sql("""
                SELECT l.api_client_id,
                       COALESCE(c.name, 'Chave ausente ou inválida') AS api_client_name,
                       t.slug AS tenant_slug,
                       w.slug AS workspace_slug,
                       COUNT(*) AS total_requests,
                       COUNT(*) FILTER (WHERE l.allowed) AS allowed_requests,
                       COUNT(*) FILTER (WHERE NOT l.allowed) AS blocked_requests,
                       MAX(l.created_at) AS last_request_at
                FROM api_client_usage_logs l
                LEFT JOIN api_clients c ON c.id = l.api_client_id
                LEFT JOIN tenants t ON t.id = l.tenant_id
                LEFT JOIN workspaces w ON w.id = l.workspace_id
                GROUP BY l.api_client_id, c.name, t.slug, w.slug
                ORDER BY MAX(l.created_at) DESC
                LIMIT :limit
                """)
                .param("limit", limit)
                .query(this::mapSummary)
                .list();
    }

    private ApiClientUsageLogEntryResponse mapEntry(ResultSet rs, int rowNum) throws SQLException {
        return new ApiClientUsageLogEntryResponse(
                rs.getObject("id", UUID.class),
                rs.getObject("api_client_id", UUID.class),
                rs.getString("api_client_name"),
                rs.getString("tenant_slug"),
                rs.getString("workspace_slug"),
                rs.getString("method"),
                rs.getString("path"),
                rs.getString("scope"),
                rs.getInt("status_code"),
                rs.getBoolean("allowed"),
                rs.getString("blocked_reason"),
                rs.getString("client_ip"),
                rs.getString("user_agent"),
                rs.getInt("latency_ms"),
                rs.getObject("created_at", OffsetDateTime.class));
    }

    private ApiClientUsageSummaryResponse mapSummary(ResultSet rs, int rowNum) throws SQLException {
        return new ApiClientUsageSummaryResponse(
                rs.getObject("api_client_id", UUID.class),
                rs.getString("api_client_name"),
                rs.getString("tenant_slug"),
                rs.getString("workspace_slug"),
                rs.getLong("total_requests"),
                rs.getLong("allowed_requests"),
                rs.getLong("blocked_requests"),
                rs.getObject("last_request_at", OffsetDateTime.class));
    }
}
