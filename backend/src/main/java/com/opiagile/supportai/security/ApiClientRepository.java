package com.opiagile.supportai.security;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.opiagile.supportai.tenant.TenantContext;

@Repository
public class ApiClientRepository {

    private final JdbcClient jdbc;

    public ApiClientRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<ApiClientRecord> findByKeyHash(String keyHash) {
        return jdbc.sql("""
                SELECT c.id,
                       c.name,
                       c.key_prefix,
                       c.key_hash,
                       c.status,
                       c.scopes,
                       c.rate_limit_per_minute,
                       c.expires_at,
                       t.id AS tenant_id,
                       t.slug AS tenant_slug,
                       w.id AS workspace_id,
                       w.slug AS workspace_slug,
                       w.name AS workspace_name
                FROM api_clients c
                JOIN tenants t ON t.id = c.tenant_id
                JOIN workspaces w ON w.id = c.workspace_id
                WHERE c.key_hash = :keyHash
                """)
                .param("keyHash", keyHash)
                .query(this::map)
                .optional();
    }

    public void markUsed(UUID clientId) {
        jdbc.sql("UPDATE api_clients SET last_used_at = now() WHERE id = :clientId")
                .param("clientId", clientId)
                .update();
    }

    public UUID create(
            TenantContext tenantContext,
            String name,
            String keyPrefix,
            String keyHash,
            Set<String> scopes,
            int rateLimitPerMinute) {
        return create(tenantContext, name, keyPrefix, keyHash, scopes, rateLimitPerMinute, null);
    }

    public UUID create(
            TenantContext tenantContext,
            String name,
            String keyPrefix,
            String keyHash,
            Set<String> scopes,
            int rateLimitPerMinute,
            OffsetDateTime expiresAt) {
        return jdbc.sql("""
                        INSERT INTO api_clients
                            (tenant_id, workspace_id, name, key_prefix, key_hash, scopes, status, rate_limit_per_minute, expires_at)
                        VALUES
                            (:tenantId, :workspaceId, :name, :keyPrefix, :keyHash, string_to_array(:scopesCsv, ',')::text[], 'ACTIVE', :rateLimit, :expiresAt)
                        RETURNING id
                        """)
                .param("tenantId", tenantContext.tenantId())
                .param("workspaceId", tenantContext.workspaceId())
                .param("name", name)
                .param("keyPrefix", keyPrefix)
                .param("keyHash", keyHash)
                .param("scopesCsv", String.join(",", scopes))
                .param("rateLimit", rateLimitPerMinute)
                .param("expiresAt", expiresAt)
                .query(UUID.class)
                .single();
    }

    private ApiClientRecord map(ResultSet rs, int rowNum) throws SQLException {
        TenantContext tenantContext = new TenantContext(
                rs.getObject("tenant_id", UUID.class),
                rs.getString("tenant_slug"),
                rs.getObject("workspace_id", UUID.class),
                rs.getString("workspace_slug"),
                rs.getString("workspace_name"));
        return new ApiClientRecord(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("key_prefix"),
                rs.getString("key_hash"),
                rs.getString("status"),
                scopes(rs.getArray("scopes")),
                rs.getInt("rate_limit_per_minute"),
                rs.getObject("expires_at", OffsetDateTime.class),
                tenantContext);
    }

    private Set<String> scopes(Array array) throws SQLException {
        if (array == null) {
            return Set.of();
        }
        String[] values = (String[]) array.getArray();
        return Arrays.stream(values)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
